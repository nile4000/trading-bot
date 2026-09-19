package ch.lueem.tradingbot.adapters.execution.binance.flow;

import java.math.BigDecimal;

import ch.lueem.tradingbot.adapters.config.paper.PaperOrderMode;
import ch.lueem.tradingbot.adapters.binance.client.BinanceOrderClient;
import ch.lueem.tradingbot.adapters.execution.binance.order.BinanceOrderRequestFactory;
import ch.lueem.tradingbot.adapters.execution.binance.order.BinanceClientOrderId;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceSymbolInfo;
import ch.lueem.tradingbot.adapters.portfolio.PaperPortfolioService;
import ch.lueem.tradingbot.core.execution.ExecutionService;
import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.execution.Result;
import ch.lueem.tradingbot.core.execution.Status;
import ch.lueem.tradingbot.core.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;

/**
 * Orchestrates Binance-backed paper execution, skip rules, and portfolio updates.
 */
public class BinancePaperExecutionService implements ExecutionService {

    private final BinanceOrderClient client;
    private final PaperOrderMode orderMode;
    private final BinanceOrderRequestFactory requestFactory;
    private final boolean placeOrdersEnabled;
    private final BigDecimal maxOrderNotional;
    private final PaperPortfolioService portfolioService;
    private final double recvWindowMillis;
    private final BinancePortfolioSync portfolioSync;
    private final BinanceClientOrderId clientOrderId;
    private final BinanceSymbolInfo symbolInfo;

    public BinancePaperExecutionService(
            BinanceOrderClient client,
            PaperPortfolioService portfolioService,
            BigDecimal orderQuantity,
            double recvWindowMillis,
            PaperOrderMode orderMode,
            boolean placeOrdersEnabled,
            BigDecimal maxOrderNotional) {
        this(client, portfolioService, orderQuantity, recvWindowMillis, orderMode,
                placeOrdersEnabled, maxOrderNotional, null, new BinanceClientOrderId(), null);
    }

    public BinancePaperExecutionService(
            BinanceOrderClient client,
            PaperPortfolioService portfolioService,
            BigDecimal orderQuantity,
            double recvWindowMillis,
            PaperOrderMode orderMode,
            boolean placeOrdersEnabled,
            BigDecimal maxOrderNotional,
            BinancePortfolioSync portfolioSync,
            BinanceClientOrderId clientOrderId) {
        this(client, portfolioService, orderQuantity, recvWindowMillis, orderMode,
                placeOrdersEnabled, maxOrderNotional, portfolioSync, clientOrderId, null);
    }

    public BinancePaperExecutionService(
            BinanceOrderClient client,
            PaperPortfolioService portfolioService,
            BigDecimal orderQuantity,
            double recvWindowMillis,
            PaperOrderMode orderMode,
            boolean placeOrdersEnabled,
            BigDecimal maxOrderNotional,
            BinancePortfolioSync portfolioSync,
            BinanceClientOrderId clientOrderId,
            BinanceSymbolInfo symbolInfo) {
        if (client == null || portfolioService == null || orderMode == null || clientOrderId == null) {
            throw new IllegalArgumentException("client, portfolioService, orderMode, and clientOrderId must not be null.");
        }
        this.client = client;
        this.portfolioService = portfolioService;
        this.orderMode = orderMode;
        this.placeOrdersEnabled = placeOrdersEnabled;
        this.maxOrderNotional = maxOrderNotional;
        this.recvWindowMillis = recvWindowMillis;
        this.portfolioSync = portfolioSync;
        this.clientOrderId = clientOrderId;
        this.symbolInfo = symbolInfo;
        this.requestFactory = new BinanceOrderRequestFactory(orderQuantity, recvWindowMillis);
    }

    @Override
    public Result execute(Request request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null.");
        }

        var snapshot = portfolioService.getSnapshot(request.symbol());
        var skippedResult = skipResultFor(request, snapshot);
        if (skippedResult != null) {
            return skippedResult;
        }

        try {
            return switch (orderMode) {
                case VALIDATE_ONLY -> validateOnly(request);
                case PLACE_ORDER -> placeOrder(request);
            };
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to %s %s action on the configured Binance paper environment for symbol %s"
                            .formatted(orderMode == PaperOrderMode.PLACE_ORDER ? "place" : "validate",
                                    request.tradeAction(), request.symbol()),
                    exception);
        }
    }

    private Result validateOnly(Request request) {
        client.validateOrder(requestFactory.buildValidationRequest(request));
        return applySuccessfulAction(request, Status.VALIDATED, "validated_only");
    }

    private Result placeOrder(Request request) {
        if (portfolioSync == null) {
            throw new IllegalStateException("Binance portfolio synchronization is required for PLACE_ORDER.");
        }
        String orderClientId = clientOrderId.create(request);
        var existingOrder = client.findOrder(request.symbol(), orderClientId, recvWindowMillis);
        if (existingOrder.isPresent()) {
            var snapshot = portfolioSync.sync();
            return new Result(Status.EXECUTED, existingOrder.get().executedQuantity().signum() > 0,
                    snapshot.position().open(), "recovered_paper_order#" + existingOrder.get().orderId());
        }

        BigDecimal quantity = request.tradeAction() == TradeAction.SELL
                ? sellableQuantity(request)
                : requestFactory.orderQuantity();
        try {
            var response = client.placeOrder(
                    requestFactory.buildOrderRequest(request, quantity, orderClientId));
            var snapshot = portfolioSync.sync();
            var detail = response.getOrderId() == null
                    ? "paper_order"
                    : "paper_order#" + response.getOrderId();
            boolean filled = response.getExecutedQty() == null
                    || new BigDecimal(response.getExecutedQty()).signum() > 0;
            return new Result(Status.EXECUTED, filled, snapshot.position().open(), detail);
        } catch (RuntimeException failure) {
            var recovered = client.findOrder(request.symbol(), orderClientId, recvWindowMillis);
            if (recovered.isEmpty()) {
                throw failure;
            }
            var snapshot = portfolioSync.sync();
            return new Result(Status.EXECUTED, recovered.get().executedQuantity().signum() > 0,
                    snapshot.position().open(), "recovered_paper_order#" + recovered.get().orderId());
        }
    }

    private BigDecimal sellableQuantity(Request request) {
        BigDecimal positionQuantity = portfolioService.getSnapshot(request.symbol()).position().quantity();
        return symbolInfo == null
                ? positionQuantity
                : symbolInfo.lotSize().sellableQuantity(positionQuantity);
    }

    private Result skipResultFor(Request request, PortfolioSnapshot snapshot) {
        if (request.tradeAction() == TradeAction.HOLD) {
            return new Result(Status.SKIPPED, false, false, "no_action");
        }
        if (request.referencePrice() == null || request.referencePrice().signum() <= 0) {
            return new Result(Status.SKIPPED, false, false, "invalid_price");
        }

        var position = snapshot.position();
        if (request.tradeAction() == TradeAction.BUY && position.open()) {
            return new Result(Status.SKIPPED, false, true, "position_already_open");
        }
        if (request.tradeAction() == TradeAction.SELL && !position.open()) {
            return new Result(Status.SKIPPED, false, false, "no_open_position");
        }
        if (request.tradeAction() == TradeAction.BUY && availableCashIsInsufficient(request, snapshot)) {
            return new Result(Status.SKIPPED, false, false, "insufficient_cash");
        }
        if (request.tradeAction() == TradeAction.BUY && exceedsMaxOrderNotional(request)) {
            return new Result(Status.SKIPPED, false, false, "max_notional_exceeded");
        }
        if (orderMode == PaperOrderMode.PLACE_ORDER && !placeOrdersEnabled) {
            return new Result(Status.SKIPPED, false, position.open(), "place_orders_disabled");
        }

        return null;
    }

    private boolean availableCashIsInsufficient(Request request, PortfolioSnapshot snapshot) {
        var orderValue = requestFactory.orderQuantity().multiply(request.referencePrice());
        return snapshot.availableCash().compareTo(orderValue) < 0;
    }

    private boolean exceedsMaxOrderNotional(Request request) {
        if (maxOrderNotional == null || maxOrderNotional.signum() <= 0) {
            return false;
        }
        var orderValue = requestFactory.orderQuantity().multiply(request.referencePrice());
        return orderValue.compareTo(maxOrderNotional) > 0;
    }

    private Result applySuccessfulAction(Request request, Status status, String message) {
        if (request.tradeAction() == TradeAction.BUY) {
            portfolioService.openPosition(
                    request.symbol(),
                    requestFactory.orderQuantity(),
                    request.referencePrice(),
                    request.requestedAt());
            return new Result(status, true, true, message);
        }

        portfolioService.closePosition(request.symbol(), request.referencePrice());
        return new Result(status, true, false, message);
    }
}
