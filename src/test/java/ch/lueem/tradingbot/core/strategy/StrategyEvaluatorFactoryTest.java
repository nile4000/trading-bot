package ch.lueem.tradingbot.core.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;

class StrategyEvaluatorFactoryTest {

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
