package ch.lueem.tradingbot.adapters.binance.client;

import java.util.Optional;

import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceOrder;
import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.NewOrderResponse;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;

/** Validates, places, and looks up Binance orders. */
public interface BinanceOrderClient {

    Optional<BinanceOrder> findOrder(String symbol, String clientOrderId, double recvWindowMillis);

    void validateOrder(OrderTestRequest request);

    NewOrderResponse placeOrder(NewOrderRequest request);
}
