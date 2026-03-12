package ch.lueem.tradingbot.adapters.config;

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
        assertEquals(false, config.paper().execution().placeOrdersEnabled());
        assertEquals("queued_actions", config.paper().strategy().name());
        assertEquals(15000.0, config.paper().binance().recvWindowMillis());
    }

    @Test
    void load_acceptsPlaceOrderPaperConfiguration() {
        ApplicationConfig config = new ApplicationConfigLoader("paper-config-place-order.yml").load();

        assertEquals(ApplicationMode.PAPER, config.mode());
        assertEquals(PaperOrderMode.PLACE_ORDER, config.paper().execution().orderMode());
        assertTrue(config.paper().execution().placeOrdersEnabled());
        assertEquals("25.0", config.paper().execution().maxOrderNotional().toPlainString());
    }

    @Test
    void load_rejectsPlaceOrderConfigurationWithoutSafetyGuards() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new ApplicationConfigLoader("paper-config-place-order-missing-guard.yml").load());

        assertTrue(exception.getMessage().contains("placeOrdersEnabled"));
    }

    @Test
    void load_acceptsPaperTa4jStrategyConfiguration() {
        ApplicationConfig config = new ApplicationConfigLoader("paper-config-ta4j-strategy.yml").load();

        assertEquals(ApplicationMode.PAPER, config.mode());
        assertEquals("ema_cross", config.paper().strategy().name());
        assertEquals(3, config.paper().strategy().parameters().shortEma());
        assertEquals(7, config.paper().strategy().parameters().longEma());
    }
}
