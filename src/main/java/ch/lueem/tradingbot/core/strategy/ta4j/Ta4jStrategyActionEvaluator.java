package ch.lueem.tradingbot.core.strategy.ta4j;

import ch.lueem.tradingbot.core.strategy.action.ActionContext;
import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

/**
 * Evaluates trade actions from ta4j rules for the current bar index.
 */
public class Ta4jStrategyActionEvaluator implements StrategyActionEvaluator {

    private final BarSeries series;
    private final Strategy strategy;

    public Ta4jStrategyActionEvaluator(BarSeries series, Strategy strategy) {
        this.series = series;
        this.strategy = strategy;
    }

    @Override
    public TradeAction evaluate(ActionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null.");
        }

        int index = context.barIndex();
        if (series == null || index < 0 || index >= series.getBarCount()) {
            return TradeAction.HOLD;
        }

        if (!context.openPosition() && strategy.shouldEnter(index)) {
            return TradeAction.BUY;
        }
        if (context.openPosition() && strategy.shouldExit(index)) {
            return TradeAction.SELL;
        }
        return TradeAction.HOLD;
    }
}
