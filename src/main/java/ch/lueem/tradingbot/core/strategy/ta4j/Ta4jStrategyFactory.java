package ch.lueem.tradingbot.core.strategy.ta4j;

import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

/**
 * Builds ta4j-backed action evaluators from strategy definitions.
 */
public class Ta4jStrategyFactory {

    private static final String EMA_CROSS = "ema_cross";
    private static final String SMA_CROSS = "sma_cross";
    private static final String RSI_REVERSION = "rsi_reversion";

    public StrategyActionEvaluator create(StrategyDefinition definition, BarSeries series) {
        return switch (definition.name()) {
            case EMA_CROSS -> createEmaCrossEvaluator(definition.parameters(), series);
            case SMA_CROSS -> createSmaCrossEvaluator(definition.parameters(), series);
            case RSI_REVERSION -> createRsiReversionEvaluator(definition.parameters(), series);
            default -> throw new IllegalStateException("Unsupported ta4j strategy: " + definition.name());
        };
    }

    private StrategyActionEvaluator createEmaCrossEvaluator(StrategyParameters parameters, BarSeries series) {
        validateShortLongParameters(parameters);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Buy when the short EMA moves above the long EMA, sell when it falls below again.
        EMAIndicator shortEma = new EMAIndicator(closePrice, parameters.shortEma());
        EMAIndicator longEma = new EMAIndicator(closePrice, parameters.longEma());
        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma);
        Rule exitRule = new CrossedDownIndicatorRule(shortEma, longEma);
        return new Ta4jStrategyActionEvaluator(series, entryRule, exitRule, parameters.longEma());
    }

    private StrategyActionEvaluator createSmaCrossEvaluator(StrategyParameters parameters, BarSeries series) {
        validateShortLongParameters(parameters);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Same rule as EMA cross, but using simple moving averages.
        SMAIndicator shortSma = new SMAIndicator(closePrice, parameters.shortEma());
        SMAIndicator longSma = new SMAIndicator(closePrice, parameters.longEma());
        Rule entryRule = new CrossedUpIndicatorRule(shortSma, longSma);
        Rule exitRule = new CrossedDownIndicatorRule(shortSma, longSma);
        return new Ta4jStrategyActionEvaluator(series, entryRule, exitRule, parameters.longEma());
    }

    private StrategyActionEvaluator createRsiReversionEvaluator(StrategyParameters parameters, BarSeries series) {
        validateRsiParameters(parameters);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Buy when RSI is below the lower threshold, sell when RSI rises above the upper threshold.
        RSIIndicator rsi = new RSIIndicator(closePrice, parameters.rsiPeriod());
        Rule entryRule = (index, tradingRecord) -> rsi.getValue(index).doubleValue() < parameters.buyBelow();
        Rule exitRule = (index, tradingRecord) -> rsi.getValue(index).doubleValue() > parameters.sellAbove();
        return new Ta4jStrategyActionEvaluator(series, entryRule, exitRule, parameters.rsiPeriod());
    }

    private void validateShortLongParameters(StrategyParameters parameters) {
        if (parameters.shortEma() == null || parameters.longEma() == null) {
            throw new IllegalArgumentException("shortEma and longEma must be configured.");
        }
        if (parameters.shortEma() <= 0 || parameters.longEma() <= 0) {
            throw new IllegalArgumentException("EMA lengths must be greater than zero.");
        }
        if (parameters.shortEma() >= parameters.longEma()) {
            throw new IllegalArgumentException("shortEma must be smaller than longEma.");
        }
    }

    private void validateRsiParameters(StrategyParameters parameters) {
        if (parameters.rsiPeriod() == null || parameters.buyBelow() == null || parameters.sellAbove() == null) {
            throw new IllegalArgumentException("rsiPeriod, buyBelow and sellAbove must be configured.");
        }
        if (parameters.rsiPeriod() <= 0) {
            throw new IllegalArgumentException("rsiPeriod must be greater than zero.");
        }
        if (parameters.buyBelow() <= 0 || parameters.buyBelow() >= 100) {
            throw new IllegalArgumentException("buyBelow must be between 1 and 99.");
        }
        if (parameters.sellAbove() <= 0 || parameters.sellAbove() >= 100) {
            throw new IllegalArgumentException("sellAbove must be between 1 and 99.");
        }
        if (parameters.buyBelow() >= parameters.sellAbove()) {
            throw new IllegalArgumentException("buyBelow must be smaller than sellAbove.");
        }
    }
}
