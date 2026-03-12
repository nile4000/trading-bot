package ch.lueem.tradingbot.adapters.execution;

import java.math.BigDecimal;

import com.binance.connector.client.common.ApiException;
import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.spot.rest.api.SpotRestApi;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;
import com.binance.connector.client.spot.rest.model.TickerPriceResponse;
import com.binance.connector.client.spot.rest.model.TickerPriceResponse1;

/**
 * Bridges the generated Binance Spot REST client to the paper bot abstractions.
 */
public class DefaultBinanceSpotTestnetClient implements BinanceSpotTestnetClient {

    private final ClientConfiguration clientConfiguration;
    private final SpotRestApi spotRestApi;

    public DefaultBinanceSpotTestnetClient(ClientConfiguration clientConfiguration) {
        if (clientConfiguration == null) {
            throw new IllegalArgumentException("clientConfiguration must not be null.");
        }
        this.clientConfiguration = clientConfiguration;
        this.spotRestApi = new SpotRestApi(clientConfiguration);
    }

    @Override
    public String baseUrl() {
        return clientConfiguration.getUrl();
    }

    @Override
    public BigDecimal loadSymbolPrice(String symbol) {
        try {
            ApiResponse<TickerPriceResponse> response = spotRestApi.tickerPrice(symbol, null, null);
            Object actualInstance = response.getData().getActualInstance();
            if (!(actualInstance instanceof TickerPriceResponse1 tickerPriceResponse)) {
                throw new IllegalStateException("Unexpected ticker response type for symbol " + symbol);
            }
            return new BigDecimal(tickerPriceResponse.getPrice());
        } catch (ApiException | NumberFormatException exception) {
            throw new IllegalStateException("Failed to load ticker price from Binance Spot Testnet for symbol "
                    + symbol, exception);
        }
    }

    @Override
    public void validateOrder(OrderTestRequest request) {
        try {
            spotRestApi.orderTest(request);
        } catch (ApiException exception) {
            throw new IllegalStateException("Binance Spot Testnet order validation failed for symbol "
                    + request.getSymbol(), exception);
        }
    }
}
