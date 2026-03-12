package ch.lueem.tradingbot.core.strategy.ta4j;

import ch.lueem.tradingbot.core.strategy.action.ActionContext;
import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;

/**
 * Evaluates trade actions from ta4j rules for the current bar index.
 */
public class Ta4jStrategyActionEvaluator implements StrategyActionEvaluator {

    private final BarSeries series;
    private final Rule entryRule;
    private final Rule exitRule;
    private final int warmupBars;

    public Ta4jStrategyActionEvaluator(BarSeries series, Rule entryRule, Rule exitRule, int warmupBars) {
        this.series = series;
        this.entryRule = entryRule;
        this.exitRule = exitRule;
        this.warmupBars = warmupBars;
    }

    @Override
    public TradeAction evaluate(ActionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null.");
        }

        BarSeries effectiveSeries = context.barSeries() != null ? context.barSeries() : series;
        int index = context.barIndex();
        if (effectiveSeries == null || index < 0 || index >= effectiveSeries.getBarCount() || index < warmupBars) {
            return TradeAction.HOLD;
        }

        if (!context.openPosition() && entryRule.isSatisfied(index)) {
            return TradeAction.BUY;
        }
        if (context.openPosition() && exitRule.isSatisfied(index)) {
            return TradeAction.SELL;
        }
        return TradeAction.HOLD;
    }
}
