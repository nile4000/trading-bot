package ch.lueem.tradingbot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApplicationConfigLoaderTest {

    @Test
    void load_acceptsNestedPaperConfiguration() {
        ApplicationConfig config = new ApplicationConfigLoader("paper-config-valid.yml").load();

        assertEquals(ApplicationMode.PAPER, config.mode());
        assertEquals("btcusdt-paper-testnet", config.paper().bot().botId());
        assertEquals(PaperOrderMode.VALIDATE_ONLY, config.paper().execution().orderMode());
        assertEquals("queued_signals", config.paper().signalSource().strategyName());
        assertEquals(15000.0, config.paper().binance().recvWindowMillis());
    }

    @Test
    void load_rejectsUnsupportedPaperOrderMode() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new ApplicationConfigLoader("paper-config-place-order.yml").load());

        assertTrue(exception.getMessage().contains("not supported in phase 1"));
    }
}
