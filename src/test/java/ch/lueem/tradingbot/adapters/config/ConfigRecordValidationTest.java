package ch.lueem.tradingbot.adapters.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.paper.PaperExchange;
import ch.lueem.tradingbot.adapters.config.paper.PaperExecutionConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperOrderMode;
import ch.lueem.tradingbot.adapters.config.paper.PaperStrategyConfig;
import org.junit.jupiter.api.Test;

class ConfigRecordValidationTest {

    @Test
    void paperStrategyConfigRequiresActionsForQueuedActions() {
        var exception = assertThrows(
                IllegalStateException.class,
                () -> new PaperStrategyConfig("queued_actions", null, List.of()));

        assertEquals("paper.strategy.actions must not be null or empty for queued_actions.", exception.getMessage());
    }

    @Test
    void paperExecutionConfigRequiresSafetyGuardsForPlaceOrderMode() {
        var exception = assertThrows(
                IllegalStateException.class,
                () -> new PaperExecutionConfig(
                        PaperExchange.BINANCE_SPOT_TESTNET,
                        PaperOrderMode.PLACE_ORDER,
                        1_000L,
                        10_000.0,
                        new BigDecimal("0.01000000"),
                        false,
                        null));

        assertEquals(
                "paper.execution.placeOrdersEnabled must be true when paper.execution.orderMode=PLACE_ORDER.",
                exception.getMessage());
    }

}
