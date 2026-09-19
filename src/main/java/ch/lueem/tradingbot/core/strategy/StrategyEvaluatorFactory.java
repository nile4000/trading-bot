package ch.lueem.tradingbot.core.strategy;

import ch.lueem.tradingbot.core.strategy.action.QueuedActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.ta4j.Ta4jStrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.ta4j.Ta4jStrategyFactory;
import jakarta.inject.Singleton;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.rules.OverIndicatorRule;

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
            default -> new Ta4jStrategyActionEvaluator(
                    context.series(),
                    ta4jStrategyFactory.create(definition, context.series()));
        };
    }

    public StrategyActionEvaluator create(
            StrategyDefinition definition,
            StrategyEvaluatorContext context,
            AdxFilterConfig adxFilter) {
        if (!adxFilter.enabled()) {
            return create(definition, context);
        }
        ADXIndicator adx = new ADXIndicator(context.series(), adxFilter.period());
        return create(
                definition,
                context,
                new OverIndicatorRule(adx, adxFilter.minimumStrength()),
                adx.getCountOfUnstableBars());
    }

    public StrategyActionEvaluator create(
            StrategyDefinition definition,
            StrategyEvaluatorContext context,
            Rule entryFilter,
            int entryFilterUnstableBars) {
        if (QUEUED_ACTIONS.equals(definition.name())) {
            throw new IllegalArgumentException("Entry filters require a ta4j strategy.");
        }
        return new Ta4jStrategyActionEvaluator(
                context.series(),
                ta4jStrategyFactory.create(
                        definition,
                        context.series(),
                        entryFilter,
                        entryFilterUnstableBars));
    }

    public int requiredHistoryBars(StrategyDefinition definition) {
        if (QUEUED_ACTIONS.equals(definition.name())) {
            return 0;
        }
        var parameters = definition.parameters();
        if (parameters == null) {
            throw new IllegalArgumentException("Strategy parameters are required for " + definition.name());
        }
        int longestPeriod = switch (definition.name()) {
            case "ema_cross", "sma_cross" -> requiredPeriod(parameters.longEma(), "longEma");
            case "rsi_reversion" -> requiredPeriod(parameters.rsiPeriod(), "rsiPeriod");
            default -> throw new IllegalStateException("Unsupported strategy: " + definition.name());
        };
        return Math.multiplyExact(3, longestPeriod);
    }

    public int requiredHistoryBars(StrategyDefinition definition, AdxFilterConfig adxFilter) {
        int strategyHistoryBars = requiredHistoryBars(definition);
        if (!adxFilter.enabled()) {
            return strategyHistoryBars;
        }
        return Math.max(strategyHistoryBars, Math.multiplyExact(3, requiredPeriod(adxFilter.period(), "period")));
    }

    private int requiredPeriod(Integer period, String fieldName) {
        if (period == null || period <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
        return period;
    }
}
