package ch.lueem.tradingbot.core.strategy.ta4j;

import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

/**
 * Builds ta4j-backed action evaluators from strategy definitions.
 */
public class Ta4jStrategyFactory {

    private static final String EMA_CROSS = "ema_cross";

    public StrategyActionEvaluator create(StrategyDefinition definition, BarSeries series) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null.");
        }
        if (series == null) {
            throw new IllegalArgumentException("series must not be null.");
        }

        return switch (definition.name()) {
            case EMA_CROSS -> createEmaCrossEvaluator(definition.parameters(), series);
            default -> throw new IllegalStateException("Unsupported ta4j strategy: " + definition.name());
        };
    }

    private StrategyActionEvaluator createEmaCrossEvaluator(StrategyParameters parameters, BarSeries series) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters must not be null.");
        }
        if (parameters.shortEma() <= 0 || parameters.longEma() <= 0) {
            throw new IllegalArgumentException("EMA lengths must be greater than zero.");
        }
        if (parameters.shortEma() >= parameters.longEma()) {
            throw new IllegalArgumentException("shortEma must be smaller than longEma.");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator shortEma = new EMAIndicator(closePrice, parameters.shortEma());
        EMAIndicator longEma = new EMAIndicator(closePrice, parameters.longEma());
        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma);
        Rule exitRule = new CrossedDownIndicatorRule(shortEma, longEma);
        return new Ta4jStrategyActionEvaluator(series, entryRule, exitRule, parameters.longEma());
    }
}
