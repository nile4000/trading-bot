package ch.lueem.tradingbot.adapters.execution.binance.client;

import java.math.BigDecimal;

import com.binance.connector.client.common.ApiException;
import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.spot.rest.api.SpotRestApi;
import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.NewOrderResponse;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;
import com.binance.connector.client.spot.rest.model.TickerPriceResponse1;

/**
 * Bridges the generated Binance Spot REST client to the paper bot abstractions.
 */
public class DefaultBinanceClient implements BinanceClient {

    private final ClientConfiguration clientConfiguration;
    private final SpotRestApi spotRestApi;

    public DefaultBinanceClient(ClientConfiguration clientConfiguration) {
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
            var response =
                    callApi(() -> spotRestApi.tickerPrice(symbol, null, null),
                            "Failed to load ticker price from Binance for symbol " + symbol);
            var actualInstance = response.getData().getActualInstance();
            if (!(actualInstance instanceof TickerPriceResponse1 tickerPriceResponse)) {
                throw new IllegalStateException("Unexpected ticker response type for symbol " + symbol);
            }
            return new BigDecimal(tickerPriceResponse.getPrice());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Failed to load ticker price from Binance for symbol " + symbol, exception);
        }
    }

    @Override
    public void validateOrder(OrderTestRequest request) {
        runApiCall(
                () -> spotRestApi.orderTest(request),
                "Binance order validation failed for symbol " + request.getSymbol());
    }

    @Override
    public NewOrderResponse placeOrder(NewOrderRequest request) {
        return callApi(
                () -> spotRestApi.newOrder(request).getData(),
                "Binance order placement failed for symbol " + request.getSymbol());
    }

    private void runApiCall(ApiRunnable action, String failureMessage) {
        try {
            action.run();
        } catch (ApiException exception) {
            throw new IllegalStateException(failureMessage, exception);
        }
    }

    private <T> T callApi(Supplier<T> action, String failureMessage) {
        try {
            return action.get();
        } catch (ApiException exception) {
            throw new IllegalStateException(failureMessage, exception);
        }
    }

    @FunctionalInterface
    private interface ApiRunnable {
        void run() throws ApiException;
    }

    @FunctionalInterface
    private interface Supplier<T> {
        T get() throws ApiException;
    }
}
