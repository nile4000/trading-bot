package ch.lueem.tradingbot.adapters.execution.binance.client;

import java.math.BigDecimal;

import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.NewOrderResponse;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;

/**
 * Minimal Binance client facade used by the paper bot.
 */
public interface BinanceClient {

    String baseUrl();

    BigDecimal loadSymbolPrice(String symbol);

    /**
     * Validates an order against Binance exchange rules without creating it.
     */
    void validateOrder(OrderTestRequest request);

    /**
     * Places an order on the configured Binance environment.
     */
    NewOrderResponse placeOrder(NewOrderRequest request);
}
