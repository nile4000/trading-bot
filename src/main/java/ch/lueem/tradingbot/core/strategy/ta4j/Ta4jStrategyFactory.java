package ch.lueem.tradingbot.core.strategy.ta4j;

import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

/**
 * Builds ta4j strategies from application strategy definitions.
 */
public class Ta4jStrategyFactory {

    private static final String EMA_CROSS = "ema_cross";
    private static final String SMA_CROSS = "sma_cross";
    private static final String RSI_REVERSION = "rsi_reversion";

    public Strategy create(StrategyDefinition definition, BarSeries series) {
        return create(definition, series, null, 0);
    }

    public Strategy create(
            StrategyDefinition definition,
            BarSeries series,
            Rule entryFilter,
            int entryFilterUnstableBars) {
        return switch (definition.name()) {
            case EMA_CROSS -> createEmaCrossStrategy(definition, series, entryFilter, entryFilterUnstableBars);
            case SMA_CROSS -> createSmaCrossStrategy(definition, series, entryFilter, entryFilterUnstableBars);
            case RSI_REVERSION -> createRsiReversionStrategy(definition, series, entryFilter, entryFilterUnstableBars);
            default -> throw new IllegalStateException("Unsupported ta4j strategy: " + definition.name());
        };
    }

    private Strategy createEmaCrossStrategy(
            StrategyDefinition definition,
            BarSeries series,
            Rule entryFilter,
            int entryFilterUnstableBars) {
        StrategyParameters parameters = definition.parameters();
        validateShortLongParameters(parameters);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Buy when the short EMA moves above the long EMA, sell when it falls below again.
        EMAIndicator shortEma = new EMAIndicator(closePrice, parameters.shortEma());
        EMAIndicator longEma = new EMAIndicator(closePrice, parameters.longEma());
        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma);
        Rule exitRule = new CrossedDownIndicatorRule(shortEma, longEma);
        return strategy(definition.name(), entryRule, exitRule, longEma.getCountOfUnstableBars(), entryFilter,
                entryFilterUnstableBars);
    }

    private Strategy createSmaCrossStrategy(
            StrategyDefinition definition,
            BarSeries series,
            Rule entryFilter,
            int entryFilterUnstableBars) {
        StrategyParameters parameters = definition.parameters();
        validateShortLongParameters(parameters);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Same rule as EMA cross, but using simple moving averages.
        SMAIndicator shortSma = new SMAIndicator(closePrice, parameters.shortEma());
        SMAIndicator longSma = new SMAIndicator(closePrice, parameters.longEma());
        Rule entryRule = new CrossedUpIndicatorRule(shortSma, longSma);
        Rule exitRule = new CrossedDownIndicatorRule(shortSma, longSma);
        return strategy(definition.name(), entryRule, exitRule, longSma.getCountOfUnstableBars(), entryFilter,
                entryFilterUnstableBars);
    }

    private Strategy createRsiReversionStrategy(
            StrategyDefinition definition,
            BarSeries series,
            Rule entryFilter,
            int entryFilterUnstableBars) {
        StrategyParameters parameters = definition.parameters();
        validateRsiParameters(parameters);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Buy when RSI is below the lower threshold, sell when RSI rises above the upper threshold.
        RSIIndicator rsi = new RSIIndicator(closePrice, parameters.rsiPeriod());
        Rule entryRule = new UnderIndicatorRule(rsi, parameters.buyBelow());
        Rule exitRule = new OverIndicatorRule(rsi, parameters.sellAbove());
        return strategy(definition.name(), entryRule, exitRule, rsi.getCountOfUnstableBars(), entryFilter,
                entryFilterUnstableBars);
    }

    private Strategy strategy(
            String name,
            Rule entryRule,
            Rule exitRule,
            int unstableBars,
            Rule entryFilter,
            int entryFilterUnstableBars) {
        Rule effectiveEntryRule = entryFilter == null ? entryRule : entryRule.and(entryFilter);
        int effectiveUnstableBars = Math.max(unstableBars, entryFilterUnstableBars);
        return new BaseStrategy(name, effectiveEntryRule, exitRule, effectiveUnstableBars);
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
