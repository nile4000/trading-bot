package ch.lueem.tradingbot.integration.binance.spottestnet;

import java.math.BigDecimal;

import ch.lueem.tradingbot.application.PaperOrderMode;
import ch.lueem.tradingbot.execution.ExecutionRequest;
import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.execution.ExecutionService;
import ch.lueem.tradingbot.execution.ExecutionStatus;
import ch.lueem.tradingbot.integration.paper.PaperPortfolioService;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.portfolio.PositionSnapshot;
import ch.lueem.tradingbot.strategy.action.TradeAction;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;
import com.binance.connector.client.spot.rest.model.OrderType;
import com.binance.connector.client.spot.rest.model.Side;

/**
 * Validates Spot Testnet orders through Binance without executing real trades.
 */
public class BinanceSpotTestnetExecutionService implements ExecutionService {

    private final BinanceSpotTestnetClient client;
    private final PaperPortfolioService portfolioService;
    private final BigDecimal orderQuantity;
    private final double recvWindowMillis;
    private final PaperOrderMode orderMode;

    public BinanceSpotTestnetExecutionService(
            BinanceSpotTestnetClient client,
            PaperPortfolioService portfolioService,
            BigDecimal orderQuantity,
            double recvWindowMillis,
            PaperOrderMode orderMode) {
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
        if (orderMode != PaperOrderMode.VALIDATE_ONLY) {
            throw new IllegalStateException("Binance Spot Testnet phase 1 supports only VALIDATE_ONLY order mode.");
        }
        this.client = client;
        this.portfolioService = portfolioService;
        this.orderQuantity = orderQuantity;
        this.recvWindowMillis = recvWindowMillis;
        this.orderMode = orderMode;
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null.");
        }

        if (request.tradeAction() == TradeAction.HOLD) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "No execution for HOLD action.");
        }

        if (request.referencePrice() == null || request.referencePrice().signum() <= 0) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "Order validation skipped because reference price is invalid.");
        }

        PortfolioSnapshot snapshot = portfolioService.getSnapshot(request.symbol());
        PositionSnapshot position = snapshot.position();
        if (request.tradeAction() == TradeAction.BUY && position.open()) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, true, "BUY ignored because a paper position is already open.");
        }
        if (request.tradeAction() == TradeAction.SELL && !position.open()) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "SELL ignored because no paper position is open.");
        }
        if (request.tradeAction() == TradeAction.BUY) {
            BigDecimal orderValue = orderQuantity.multiply(request.referencePrice());
            if (snapshot.availableCash().compareTo(orderValue) < 0) {
                return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "BUY ignored because available paper cash is insufficient.");
            }
        }

        OrderTestRequest orderTestRequest = new OrderTestRequest()
                .symbol(request.symbol())
                .side(toSide(request.tradeAction()))
                .type(OrderType.MARKET)
                .quantity(orderQuantity.doubleValue())
                .recvWindow(recvWindowMillis);

        try {
            client.validateOrder(orderTestRequest);
            if (request.tradeAction() == TradeAction.BUY) {
                portfolioService.openPosition(request.symbol(), orderQuantity, request.referencePrice(), request.requestedAt());
                return new ExecutionResult(
                        ExecutionStatus.VALIDATED,
                        true,
                        true,
                        "Order validated on Binance Spot Testnet and applied to the local paper portfolio via %s/orderTest."
                                .formatted(orderMode));
            }

            portfolioService.closePosition(request.symbol(), request.referencePrice());
            return new ExecutionResult(
                    ExecutionStatus.VALIDATED,
                    true,
                    false,
                    "Order validated on Binance Spot Testnet and applied to the local paper portfolio via %s/orderTest."
                            .formatted(orderMode));
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to validate %s action on Binance Spot Testnet for symbol %s"
                            .formatted(request.tradeAction(), request.symbol()),
                    exception);
        }
    }

    private Side toSide(TradeAction tradeAction) {
        return switch (tradeAction) {
            case BUY -> Side.BUY;
            case SELL -> Side.SELL;
            case HOLD -> throw new IllegalArgumentException("HOLD must not be mapped to a Binance order.");
        };
    }
}
