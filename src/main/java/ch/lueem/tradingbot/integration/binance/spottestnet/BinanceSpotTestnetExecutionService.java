package ch.lueem.tradingbot.integration.binance.spottestnet;

import java.math.BigDecimal;

import ch.lueem.tradingbot.application.PaperOrderMode;
import ch.lueem.tradingbot.execution.ExecutionRequest;
import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.execution.ExecutionService;
import ch.lueem.tradingbot.execution.ExecutionStatus;
import ch.lueem.tradingbot.strategy.signal.TradeSignal;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;
import com.binance.connector.client.spot.rest.model.OrderType;
import com.binance.connector.client.spot.rest.model.Side;

/**
 * Validates Spot Testnet orders through Binance without executing real trades.
 */
public class BinanceSpotTestnetExecutionService implements ExecutionService {

    private final BinanceSpotTestnetClient client;
    private final BigDecimal orderQuantity;
    private final double recvWindowMillis;
    private final PaperOrderMode orderMode;

    public BinanceSpotTestnetExecutionService(
            BinanceSpotTestnetClient client,
            BigDecimal orderQuantity,
            double recvWindowMillis,
            PaperOrderMode orderMode) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null.");
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
        this.orderQuantity = orderQuantity;
        this.recvWindowMillis = recvWindowMillis;
        this.orderMode = orderMode;
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null.");
        }

        if (request.tradeSignal() == TradeSignal.HOLD) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "No execution for HOLD signal.");
        }

        if (request.referencePrice() == null || request.referencePrice().signum() <= 0) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "Order validation skipped because reference price is invalid.");
        }

        OrderTestRequest orderTestRequest = new OrderTestRequest()
                .symbol(request.symbol())
                .side(toSide(request.tradeSignal()))
                .type(OrderType.MARKET)
                .quantity(orderQuantity.doubleValue())
                .recvWindow(recvWindowMillis);

        try {
            client.validateOrder(orderTestRequest);
            return new ExecutionResult(
                    ExecutionStatus.VALIDATED,
                    false,
                    false,
                    "Order validated on Binance Spot Testnet via %s/orderTest without local position changes."
                            .formatted(orderMode));
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to validate %s order on Binance Spot Testnet for symbol %s"
                            .formatted(request.tradeSignal(), request.symbol()),
                    exception);
        }
    }

    private Side toSide(TradeSignal tradeSignal) {
        return switch (tradeSignal) {
            case BUY -> Side.BUY;
            case SELL -> Side.SELL;
            case HOLD -> throw new IllegalArgumentException("HOLD must not be mapped to a Binance order.");
        };
    }
}
