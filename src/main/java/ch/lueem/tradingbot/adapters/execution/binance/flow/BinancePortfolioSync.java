package ch.lueem.tradingbot.adapters.execution.binance.flow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.adapters.binance.client.BinanceAccountClient;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceBalance;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceFill;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceOrder;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceSymbolInfo;
import ch.lueem.tradingbot.adapters.execution.binance.order.BinanceClientOrderId;
import ch.lueem.tradingbot.adapters.portfolio.PaperPortfolioService;
import ch.lueem.tradingbot.core.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.core.portfolio.PositionSnapshot;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.jboss.logging.Logger;

/** Rebuilds the bot portfolio from its Binance paper-environment orders and trades. */
public class BinancePortfolioSync {

    private static final Logger LOG = Logger.getLogger(BinancePortfolioSync.class);
    private static final int MONEY_SCALE = 4;
    private static final int QUANTITY_SCALE = 8;

    private final BinanceAccountClient client;
    private final PaperPortfolioService portfolioService;
    private final PaperConfig paper;
    private final BinanceSymbolInfo symbolInfo;
    private final BinanceClientOrderId clientOrderId;

    public BinancePortfolioSync(
            BinanceAccountClient client,
            PaperPortfolioService portfolioService,
            PaperConfig paper,
            BinanceSymbolInfo symbolInfo,
            BinanceClientOrderId clientOrderId) {
        this.client = client;
        this.portfolioService = portfolioService;
        this.paper = paper;
        this.symbolInfo = symbolInfo;
        this.clientOrderId = clientOrderId;
    }

    public synchronized PortfolioSnapshot sync() {
        List<BinanceFill> fills = ownedFills();
        List<BinanceBalance> balances = client.loadBalances(paper.binance().recvWindowMillis());
        PortfolioSnapshot snapshot = reconstructPortfolio(fills, balances);
        validateBaseBalance(snapshot, balances);
        portfolioService.replaceSnapshot(snapshot);
        return snapshot;
    }

    private List<BinanceFill> ownedFills() {
        var ownedOrderIds = new HashSet<Long>();
        ownedOrders().forEach(order -> ownedOrderIds.add(order.orderId()));
        return client.loadTrades(
                        paper.bot().symbol(), paper.binance().recvWindowMillis()).stream()
                .filter(fill -> ownedOrderIds.contains(fill.orderId()))
                .sorted(Comparator.comparing(BinanceFill::executedAt).thenComparingLong(BinanceFill::tradeId))
                .toList();
    }

    private List<BinanceOrder> ownedOrders() {
        String prefix = clientOrderId.prefix(paper.bot().botId());
        return client.loadOrders(paper.bot().symbol(), paper.binance().recvWindowMillis()).stream()
                .filter(order -> order.clientOrderId() != null && order.clientOrderId().startsWith(prefix))
                .toList();
    }

    private PortfolioSnapshot reconstructPortfolio(List<BinanceFill> fills, List<BinanceBalance> balances) {
        ReconstructedPortfolio portfolio = initialPortfolio();
        for (BinanceFill fill : fills) {
            applyFill(fill, portfolio);
        }
        validateCash(portfolio.cash);
        return createSnapshot(portfolio, balances);
    }

    private ReconstructedPortfolio initialPortfolio() {
        return new ReconstructedPortfolio(paper.execution().initialCash());
    }

    private void applyFill(BinanceFill fill, ReconstructedPortfolio portfolio) {
        if (fill.action() == TradeAction.BUY) {
            applyBuy(fill, portfolio);
        } else {
            applySell(fill, portfolio);
        }
    }

    private void applyBuy(BinanceFill fill, ReconstructedPortfolio portfolio) {
        BigDecimal acquired = fill.quantity();
        BigDecimal cost = fill.quoteQuantity();
        if (symbolInfo.baseAsset().equals(fill.commissionAsset())) {
            acquired = acquired.subtract(fill.commission());
        } else if (symbolInfo.quoteAsset().equals(fill.commissionAsset())) {
            cost = cost.add(fill.commission());
        }
        if (portfolio.quantity.signum() == 0) {
            portfolio.openedAt = fill.executedAt();
        }
        portfolio.quantity = portfolio.quantity.add(acquired);
        portfolio.costBasis = portfolio.costBasis.add(cost);
        portfolio.cash = portfolio.cash.subtract(cost);
    }

    private void applySell(BinanceFill fill, ReconstructedPortfolio portfolio) {
        BigDecimal inventoryReduction = fill.quantity();
        BigDecimal proceeds = fill.quoteQuantity();
        if (symbolInfo.baseAsset().equals(fill.commissionAsset())) {
            inventoryReduction = inventoryReduction.add(fill.commission());
        } else if (symbolInfo.quoteAsset().equals(fill.commissionAsset())) {
            proceeds = proceeds.subtract(fill.commission());
        }
        validateInventory(portfolio.quantity, inventoryReduction);
        BigDecimal removedCost = portfolio.costBasis.multiply(inventoryReduction)
                .divide(portfolio.quantity, 16, RoundingMode.HALF_UP);
        portfolio.quantity = portfolio.quantity.subtract(inventoryReduction);
        portfolio.costBasis = portfolio.costBasis.subtract(removedCost);
        portfolio.cash = portfolio.cash.add(proceeds);
        if (portfolio.quantity.signum() == 0) {
            portfolio.costBasis = BigDecimal.ZERO;
            portfolio.openedAt = null;
        }
    }

    private void validateInventory(BigDecimal quantity, BigDecimal inventoryReduction) {
        if (inventoryReduction.compareTo(quantity) > 0) {
            throw new IllegalStateException(
                    "Binance fills sell more %s than the bot owns for %s."
                            .formatted(symbolInfo.baseAsset(), paper.bot().botId()));
        }
    }

    private void validateCash(BigDecimal cash) {
        if (cash.signum() < 0) {
            throw new IllegalStateException(
                    "Bot trade history exceeds configured initial cash for " + paper.bot().botId());
        }
    }

    private PortfolioSnapshot createSnapshot(
            ReconstructedPortfolio portfolio, List<BinanceBalance> balances) {
        BigDecimal exchangeQuoteCash = balanceFor(balances, symbolInfo.quoteAsset()).free();
        BigDecimal availableCash = portfolio.cash.min(exchangeQuoteCash)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        boolean angelsShare = portfolio.quantity.signum() > 0
                && symbolInfo.lotSize().sellableQuantity(portfolio.quantity).signum() == 0;
        if (angelsShare) {
            LOG.infof("Treating non-tradable remainder as AngelsShare. botId=%s, symbol=%s, quantity=%s",
                    paper.bot().botId(), paper.bot().symbol(), portfolio.quantity.toPlainString());
        }
        PositionSnapshot position = portfolio.quantity.signum() == 0 || angelsShare
                ? PositionSnapshot.flat()
                : new PositionSnapshot(
                        true,
                        portfolio.quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP),
                        portfolio.costBasis.divide(portfolio.quantity, MONEY_SCALE, RoundingMode.HALF_UP),
                        portfolio.openedAt);
        return new PortfolioSnapshot(paper.bot().symbol(), availableCash, position);
    }

    private void validateBaseBalance(PortfolioSnapshot snapshot, List<BinanceBalance> balances) {
        if (!snapshot.position().open()) {
            return;
        }
        BigDecimal exchangeQuantity = balanceFor(balances, symbolInfo.baseAsset()).total();
        if (exchangeQuantity.compareTo(snapshot.position().quantity()) < 0) {
            throw new IllegalStateException(
                    "Binance %s balance %s is smaller than reconstructed bot position %s."
                            .formatted(symbolInfo.baseAsset(), exchangeQuantity, snapshot.position().quantity()));
        }
    }

    private BinanceBalance balanceFor(List<BinanceBalance> balances, String asset) {
        return balances.stream()
                .filter(balance -> asset.equals(balance.asset()))
                .findFirst()
                .orElse(new BinanceBalance(asset, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private static final class ReconstructedPortfolio {
        private BigDecimal cash;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal costBasis = BigDecimal.ZERO;
        private OffsetDateTime openedAt;

        private ReconstructedPortfolio(BigDecimal cash) {
            this.cash = cash;
        }
    }
}
