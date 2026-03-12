package ch.lueem.tradingbot.integration.binance.spottestnet;

import java.math.BigDecimal;

import com.binance.connector.client.spot.rest.model.OrderTestRequest;

/**
 * Minimal Spot Testnet client facade used by the paper bot.
 */
public interface BinanceSpotTestnetClient {

    String baseUrl();

    BigDecimal loadSymbolPrice(String symbol);

    void validateOrder(OrderTestRequest request);
}
