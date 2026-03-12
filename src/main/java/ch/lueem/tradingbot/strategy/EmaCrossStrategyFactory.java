package ch.lueem.tradingbot.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

/**
 * Builds the EMA cross strategy used by the local backtest runner.
 */
public class EmaCrossStrategyFactory {

    public Strategy create(BarSeries series, int shortLength, int longLength) {
        validateInputs(series, shortLength, longLength);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator shortEma = new EMAIndicator(closePrice, shortLength);
        EMAIndicator longEma = new EMAIndicator(closePrice, longLength);

        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma);
        Rule exitRule = new CrossedDownIndicatorRule(shortEma, longEma);

        return new BaseStrategy(
                "EMA-%d-%d-Cross".formatted(shortLength, longLength),
                entryRule,
                exitRule);
    }

    private void validateInputs(BarSeries series, int shortLength, int longLength) {
        if (series == null) {
            throw new IllegalArgumentException("Bar series must not be null.");
        }
        if (shortLength <= 0 || longLength <= 0) {
            throw new IllegalArgumentException("EMA lengths must be greater than zero.");
        }
        if (shortLength >= longLength) {
            throw new IllegalArgumentException("shortLength must be smaller than longLength.");
        }
    }
}
