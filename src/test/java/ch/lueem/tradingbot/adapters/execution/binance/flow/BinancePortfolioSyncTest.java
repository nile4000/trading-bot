package ch.lueem.tradingbot.adapters.execution.binance.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.paper.BinanceConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperBotConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperExchange;
import ch.lueem.tradingbot.adapters.config.paper.PaperExecutionConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperOrderMode;
import ch.lueem.tradingbot.adapters.config.paper.PaperStrategyConfig;
import ch.lueem.tradingbot.adapters.binance.client.BinanceAccountClient;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceBalance;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceFill;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceLotSize;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceOrder;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceSymbolInfo;
import ch.lueem.tradingbot.adapters.execution.binance.order.BinanceClientOrderId;
import ch.lueem.tradingbot.adapters.portfolio.PaperPortfolioService;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;

class BinancePortfolioSyncTest {

    @Test
    void rebuildsPartialPositionFromOwnedFillsAndIgnoresForeignTrades() {
        var orderId = new BinanceClientOrderId();
        String ownedPrefix = orderId.prefix("bot-1");
        var client = new ReconciliationClient(
                List.of(
                        order(1, ownedPrefix + "-buy", TradeAction.BUY),
                        order(2, ownedPrefix + "-sell", TradeAction.SELL),
                        order(3, "manual-order", TradeAction.BUY)),
                List.of(
                        fill(11, 1, TradeAction.BUY, "500.00", "0.010", "0.00001", "BTC", "10:00"),
                        fill(12, 2, TradeAction.SELL, "204.00", "0.004", "0.204", "USDT", "10:01"),
                        fill(13, 3, TradeAction.BUY, "999.00", "1.000", "0", "BTC", "10:02")));
        var portfolio = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000"));
        var portfolioSync = new BinancePortfolioSync(
                client,
                portfolio,
                config(),
                symbolInfo(),
                orderId);

        var snapshot = portfolioSync.sync();

        assertTrue(snapshot.position().open());
        assertEquals(new BigDecimal("0.00599000"), snapshot.position().quantity());
        assertEquals(new BigDecimal("50050.0501"), snapshot.position().entryPrice());
        assertEquals(new BigDecimal("703.7960"), snapshot.availableCash());
    }

    @Test
    void treatsRemainderBelowLotSizeAsAngelsShare() {
        var orderId = new BinanceClientOrderId();
        String ownedPrefix = orderId.prefix("bot-1");
        var client = new ReconciliationClient(
                List.of(
                        order(1, ownedPrefix + "-buy", TradeAction.BUY),
                        order(2, ownedPrefix + "-sell", TradeAction.SELL)),
                List.of(
                        fill(11, 1, TradeAction.BUY, "8.128001", "0.00010000",
                                "0.00000010", "BTC", "10:00"),
                        fill(12, 2, TradeAction.SELL, "7.315200", "0.00009000",
                                "0", "USDT", "10:01")));
        var portfolio = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000"));
        var portfolioSync = new BinancePortfolioSync(client, portfolio, config(), symbolInfo(), orderId);

        var snapshot = portfolioSync.sync();

        assertFalse(snapshot.position().open());
    }

    private BinanceSymbolInfo symbolInfo() {
        return new BinanceSymbolInfo(
                "BTCUSDT", "BTC", "USDT",
                new BinanceLotSize(new BigDecimal("0.00001000"), new BigDecimal("0.00001000")));
    }

    private BinanceOrder order(long id, String clientId, TradeAction action) {
        return new BinanceOrder(id, clientId, "BTCUSDT", action, "FILLED",
                new BigDecimal("0.01"), new BigDecimal("0.01"), new BigDecimal("500"),
                OffsetDateTime.parse("2026-09-19T10:00:00Z"));
    }

    private BinanceFill fill(
            long tradeId,
            long orderId,
            TradeAction action,
            String quote,
            String quantity,
            String commission,
            String commissionAsset,
            String minute) {
        return new BinanceFill(tradeId, orderId, "BTCUSDT", action,
                new BigDecimal("50000"), new BigDecimal(quantity), new BigDecimal(quote),
                new BigDecimal(commission), commissionAsset,
                OffsetDateTime.parse("2026-09-19T" + minute + ":00Z"));
    }

    private PaperConfig config() {
        return new PaperConfig(
                new PaperBotConfig("bot-1", "v1", "BTCUSDT", "1m"),
                new PaperExecutionConfig(PaperExchange.BINANCE_SPOT_DEMO, PaperOrderMode.PLACE_ORDER,
                        10_000, 1000, new BigDecimal("0.001"), true, new BigDecimal("100")),
                new PaperStrategyConfig("ema_cross", new StrategyParameters(3, 7), null),
                new BinanceConfig("key", "secret", 15_000));
    }

    private static final class ReconciliationClient implements BinanceAccountClient {
        private final List<BinanceOrder> orders;
        private final List<BinanceFill> fills;

        private ReconciliationClient(List<BinanceOrder> orders, List<BinanceFill> fills) {
            this.orders = orders;
            this.fills = fills;
        }

        @Override
        public List<BinanceOrder> loadOrders(String symbol, double recvWindowMillis) {
            return orders;
        }

        @Override
        public List<BinanceFill> loadTrades(String symbol, double recvWindowMillis) {
            return fills;
        }

        @Override
        public List<BinanceBalance> loadBalances(double recvWindowMillis) {
            return List.of(
                    new BinanceBalance("BTC", new BigDecimal("10"), BigDecimal.ZERO),
                    new BinanceBalance("USDT", new BigDecimal("10000"), BigDecimal.ZERO));
        }

    }
}
