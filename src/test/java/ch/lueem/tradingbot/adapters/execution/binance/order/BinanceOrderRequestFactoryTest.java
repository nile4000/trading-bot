package ch.lueem.tradingbot.adapters.execution.binance.order;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.junit.jupiter.api.Test;

class BinanceOrderRequestFactoryTest {

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

        assertThrows(IllegalArgumentException.class, () -> factory.buildOrderRequest(request));
    }
}
