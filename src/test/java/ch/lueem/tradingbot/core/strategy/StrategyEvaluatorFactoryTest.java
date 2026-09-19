package ch.lueem.tradingbot.core.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;

class StrategyEvaluatorFactoryTest {

    @Test
    void create_rejectsUnknownName() {
        StrategyEvaluatorFactory factory = new StrategyEvaluatorFactory();

        assertThrows(
                IllegalStateException.class,
                () -> factory.create(new StrategyDefinition("unknown", null), StrategyEvaluatorContext.queued(List.of())));
    }

    @Test
    void requiredHistoryBars_usesThreeTimesTheLongestIndicatorPeriod() {
        StrategyEvaluatorFactory factory = new StrategyEvaluatorFactory();

        assertEquals(21, factory.requiredHistoryBars(
                new StrategyDefinition("ema_cross", new StrategyParameters(3, 7))));
        assertEquals(42, factory.requiredHistoryBars(
                new StrategyDefinition("rsi_reversion", StrategyParameters.rsiReversion(14, 30, 70))));
        assertEquals(0, factory.requiredHistoryBars(new StrategyDefinition("queued_actions", null)));
    }

}
