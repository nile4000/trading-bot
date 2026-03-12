package ch.lueem.tradingbot.adapters.execution;

import java.math.BigDecimal;

import ch.lueem.tradingbot.adapters.config.PaperOrderMode;
import ch.lueem.tradingbot.core.execution.ExecutionRequest;
import ch.lueem.tradingbot.core.execution.ExecutionResult;
import ch.lueem.tradingbot.core.execution.ExecutionService;
import ch.lueem.tradingbot.core.execution.ExecutionStatus;
import ch.lueem.tradingbot.adapters.portfolio.PaperPortfolioService;
import ch.lueem.tradingbot.core.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.core.portfolio.PositionSnapshot;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.NewOrderResponse;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;
import com.binance.connector.client.spot.rest.model.OrderType;
import com.binance.connector.client.spot.rest.model.Side;

/**
 * Validates or places Spot Testnet orders and mirrors successful actions into
 * the local paper portfolio.
 */
public class BinanceSpotTestnetExecutionService implements ExecutionService {

    private final BinanceSpotTestnetClient client;
    private final PaperPortfolioService portfolioService;
    private final BigDecimal orderQuantity;
    private final double recvWindowMillis;
    private final PaperOrderMode orderMode;
    private final boolean placeOrdersEnabled;
    private final BigDecimal maxOrderNotional;

    public BinanceSpotTestnetExecutionService(
            BinanceSpotTestnetClient client,
            PaperPortfolioService portfolioService,
            BigDecimal orderQuantity,
            double recvWindowMillis,
            PaperOrderMode orderMode,
            boolean placeOrdersEnabled,
            BigDecimal maxOrderNotional) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null.");
        }
        if (portfolioService == null) {
            throw new IllegalArgumentException("portfolioService must not be null.");
        }
        if (orderQuantity == null || orderQuantity.signum() <= 0) {
            throw new IllegalArgumentException("orderQuantity must be greater than zero.");
        }
        if (recvWindowMillis <= 0.0) {
            throw new IllegalArgumentException("recvWindowMillis must be greater than zero.");
        }
        if (orderMode == null) {
            throw new IllegalArgumentException("orderMode must not be null.");
        }
        this.client = client;
        this.portfolioService = portfolioService;
        this.orderQuantity = orderQuantity;
        this.recvWindowMillis = recvWindowMillis;
        this.orderMode = orderMode;
        this.placeOrdersEnabled = placeOrdersEnabled;
        this.maxOrderNotional = maxOrderNotional;
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null.");
        }

        PortfolioSnapshot snapshot = portfolioService.getSnapshot(request.symbol());
        ExecutionResult skippedResult = skipResultFor(request, snapshot);
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
                    "Failed to %s %s action on Binance Spot Testnet for symbol %s"
                            .formatted(orderMode == PaperOrderMode.PLACE_ORDER ? "place" : "validate",
                                    request.tradeAction(), request.symbol()),
                    exception);
        }
    }

    private ExecutionResult skipResultFor(ExecutionRequest request, PortfolioSnapshot snapshot) {
        if (request.tradeAction() == TradeAction.HOLD) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "no_action");
        }
        if (request.referencePrice() == null || request.referencePrice().signum() <= 0) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "invalid_price");
        }

        PositionSnapshot position = snapshot.position();
        if (request.tradeAction() == TradeAction.BUY && position.open()) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, true, "position_already_open");
        }
        if (request.tradeAction() == TradeAction.SELL && !position.open()) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "no_open_position");
        }
        if (request.tradeAction() == TradeAction.BUY && availableCashIsInsufficient(request, snapshot)) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "insufficient_cash");
        }
        if (request.tradeAction() == TradeAction.BUY && exceedsMaxOrderNotional(request)) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "max_notional_exceeded");
        }
        if (orderMode == PaperOrderMode.PLACE_ORDER && !placeOrdersEnabled) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, position.open(), "place_orders_disabled");
        }

        return null;
    }

    private boolean availableCashIsInsufficient(ExecutionRequest request, PortfolioSnapshot snapshot) {
        BigDecimal orderValue = orderQuantity.multiply(request.referencePrice());
        return snapshot.availableCash().compareTo(orderValue) < 0;
    }

    private boolean exceedsMaxOrderNotional(ExecutionRequest request) {
        if (maxOrderNotional == null || maxOrderNotional.signum() <= 0) {
            return false;
        }
        BigDecimal orderValue = orderQuantity.multiply(request.referencePrice());
        return orderValue.compareTo(maxOrderNotional) > 0;
    }

    private OrderTestRequest buildOrderTestRequest(ExecutionRequest request) {
        return new OrderTestRequest()
                .symbol(request.symbol())
                .side(toSide(request.tradeAction()))
                .type(OrderType.MARKET)
                .quantity(orderQuantity.doubleValue())
                .recvWindow(recvWindowMillis);
    }

    private NewOrderRequest buildOrderRequest(ExecutionRequest request) {
        return new NewOrderRequest()
                .symbol(request.symbol())
                .side(toSide(request.tradeAction()))
                .type(OrderType.MARKET)
                .quantity(orderQuantity.doubleValue())
                .recvWindow(recvWindowMillis);
    }

    private ExecutionResult validateOnly(ExecutionRequest request) {
        client.validateOrder(buildOrderTestRequest(request));
        return applySuccessfulAction(request, ExecutionStatus.VALIDATED, "validated_only");
    }

    private ExecutionResult placeOrder(ExecutionRequest request) {
        NewOrderResponse response = client.placeOrder(buildOrderRequest(request));
        String detail = response.getOrderId() == null
                ? "testnet_order"
                : "testnet_order#" + response.getOrderId();
        return applySuccessfulAction(request, ExecutionStatus.EXECUTED, detail);
    }

    private ExecutionResult applySuccessfulAction(
            ExecutionRequest request,
            ExecutionStatus status,
            String message) {
        if (request.tradeAction() == TradeAction.BUY) {
            portfolioService.openPosition(request.symbol(), orderQuantity, request.referencePrice(),
                    request.requestedAt());
            return new ExecutionResult(
                    status,
                    true,
                    true,
                    message);
        }

        portfolioService.closePosition(request.symbol(), request.referencePrice());
        return new ExecutionResult(
                status,
                true,
                false,
                message);
    }

    private Side toSide(TradeAction tradeAction) {
        return switch (tradeAction) {
            case BUY -> Side.BUY;
            case SELL -> Side.SELL;
            case HOLD -> throw new IllegalArgumentException("HOLD must not be mapped to a Binance order.");
        };
    }
}
