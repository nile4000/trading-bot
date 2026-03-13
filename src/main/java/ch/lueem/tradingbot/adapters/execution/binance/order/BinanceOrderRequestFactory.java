package ch.lueem.tradingbot.adapters.execution.binance.order;

import java.math.BigDecimal;

import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;
import com.binance.connector.client.spot.rest.model.OrderType;
import com.binance.connector.client.spot.rest.model.Side;

/**
 * Builds Binance market-order requests from execution requests.
 */
public class BinanceOrderRequestFactory {

    private final BigDecimal orderQuantity;
    private final double recvWindowMillis;

    public BinanceOrderRequestFactory(BigDecimal orderQuantity, double recvWindowMillis) {
        if (orderQuantity == null || orderQuantity.signum() <= 0) {
            throw new IllegalArgumentException("orderQuantity must be greater than zero.");
        }
        if (recvWindowMillis <= 0.0) {
            throw new IllegalArgumentException("recvWindowMillis must be greater than zero.");
        }
        this.orderQuantity = orderQuantity;
        this.recvWindowMillis = recvWindowMillis;
    }

    public OrderTestRequest buildValidationRequest(Request request) {
        return new OrderTestRequest()
                .symbol(request.symbol())
                .side(toSide(request.tradeAction()))
                .type(OrderType.MARKET)
                .quantity(orderQuantity.doubleValue())
                .recvWindow(recvWindowMillis);
    }

    public NewOrderRequest buildOrderRequest(Request request) {
        return new NewOrderRequest()
                .symbol(request.symbol())
                .side(toSide(request.tradeAction()))
                .type(OrderType.MARKET)
                .quantity(orderQuantity.doubleValue())
                .recvWindow(recvWindowMillis);
    }

    public BigDecimal orderQuantity() {
        return orderQuantity;
    }

    private Side toSide(TradeAction tradeAction) {
        return switch (tradeAction) {
            case BUY -> Side.BUY;
            case SELL -> Side.SELL;
            case HOLD -> throw new IllegalArgumentException("HOLD must not be mapped to a Binance order.");
        };
    }
}
