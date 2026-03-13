package ch.lueem.tradingbot.core.strategy;

import ch.lueem.tradingbot.core.strategy.action.QueuedActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.ta4j.Ta4jStrategyFactory;
import jakarta.inject.Singleton;

/**
 * Central entry point for building strategy evaluators across all modes.
 */
@Singleton
public class StrategyEvaluatorFactory {

    private static final String QUEUED_ACTIONS = "queued_actions";

    private final Ta4jStrategyFactory ta4jStrategyFactory;

    public StrategyEvaluatorFactory() {
        this(new Ta4jStrategyFactory());
    }

    public StrategyEvaluatorFactory(Ta4jStrategyFactory ta4jStrategyFactory) {
        this.ta4jStrategyFactory = ta4jStrategyFactory;
    }

    public StrategyActionEvaluator create(StrategyDefinition definition, StrategyEvaluatorContext context) {
        return switch (definition.name()) {
            case QUEUED_ACTIONS -> new QueuedActionEvaluator(context.queuedActions());
            default -> ta4jStrategyFactory.create(definition, context.series());
        };
    }
}
