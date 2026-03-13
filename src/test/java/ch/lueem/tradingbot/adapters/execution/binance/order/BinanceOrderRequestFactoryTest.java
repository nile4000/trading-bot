package ch.lueem.tradingbot.adapters.execution.binance.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import com.binance.connector.client.spot.rest.model.Side;
import org.junit.jupiter.api.Test;

class BinanceOrderRequestFactoryTest {

    @Test
    void buildsValidationRequestFromBuyExecutionRequest() {
        BinanceOrderRequestFactory factory =
                new BinanceOrderRequestFactory(new BigDecimal("0.015"), 5_000.0);

        Request request = new Request(
                "runtime-1",
                "BTCUSDT",
                "1m",
                TradeAction.BUY,
                OffsetDateTime.parse("2026-03-13T10:15:00Z"),
                new BigDecimal("83000.12"));

        var orderRequest = factory.buildValidationRequest(request);

        assertEquals("BTCUSDT", orderRequest.getSymbol());
        assertEquals(Side.BUY, orderRequest.getSide());
        assertEquals(0.015d, orderRequest.getQuantity());
        assertEquals(5_000.0d, orderRequest.getRecvWindow());
    }

    @Test
    void buildsOrderRequestFromSellExecutionRequest() {
        BinanceOrderRequestFactory factory =
                new BinanceOrderRequestFactory(new BigDecimal("0.25"), 2_500.0);

        Request request = new Request(
                "runtime-1",
                "ETHUSDT",
                "5m",
                TradeAction.SELL,
                OffsetDateTime.parse("2026-03-13T10:15:00Z"),
                new BigDecimal("2200.50"));

        var orderRequest = factory.buildOrderRequest(request);

        assertEquals("ETHUSDT", orderRequest.getSymbol());
        assertEquals(Side.SELL, orderRequest.getSide());
        assertEquals(0.25d, orderRequest.getQuantity());
        assertEquals(2_500.0d, orderRequest.getRecvWindow());
    }

    @Test
    void rejectsHoldActionForBinanceOrders() {
        BinanceOrderRequestFactory factory =
                new BinanceOrderRequestFactory(new BigDecimal("0.1"), 1_000.0);

        Request request = new Request(
                "runtime-1",
                "BTCUSDT",
                "1h",
                TradeAction.HOLD,
                OffsetDateTime.parse("2026-03-13T10:15:00Z"),
                new BigDecimal("80000"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> factory.buildOrderRequest(request));

        assertEquals("HOLD must not be mapped to a Binance order.", exception.getMessage());
    }
}
