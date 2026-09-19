package ch.lueem.tradingbot.adapters.execution.binance.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.junit.jupiter.api.Test;

class BinanceClientOrderIdTest {

    @Test
    void createsStableIdsPerBotBarAndAction() {
        var factory = new BinanceClientOrderId();
        var buy = request(TradeAction.BUY, "2026-09-19T10:01:00Z");

        String first = factory.create(buy);
        String second = factory.create(buy);
        String sell = factory.create(request(TradeAction.SELL, "2026-09-19T10:01:00Z"));

        assertEquals(first, second);
        assertNotEquals(first, sell);
        assertTrue(first.startsWith(factory.prefix("bot-1")));
        assertTrue(first.length() <= 36);
    }

    private Request request(TradeAction action, String requestedAt) {
        return new Request(
                "bot-1", "BTCUSDT", "1m", action,
                OffsetDateTime.parse(requestedAt), new BigDecimal("80000"));
    }
}
