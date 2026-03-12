package ch.lueem.tradingbot.adapters.execution;

import java.math.BigDecimal;

import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.NewOrderResponse;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;

/**
 * Minimal Spot Testnet client facade used by the paper bot.
 */
public interface BinanceSpotTestnetClient {

    String baseUrl();

    BigDecimal loadSymbolPrice(String symbol);

    void validateOrder(OrderTestRequest request);

    NewOrderResponse placeOrder(NewOrderRequest request);
}
