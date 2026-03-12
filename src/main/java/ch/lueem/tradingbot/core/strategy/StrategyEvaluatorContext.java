package ch.lueem.tradingbot.core.strategy;

import java.util.List;

import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.ta4j.core.BarSeries;

/**
 * Holds the optional inputs needed to build one concrete strategy evaluator.
 */
public record StrategyEvaluatorContext(
        BarSeries series,
        List<TradeAction> queuedActions
) {
    public static StrategyEvaluatorContext ta4j(BarSeries series) {
        return new StrategyEvaluatorContext(series, List.of());
    }

    public static StrategyEvaluatorContext queued(List<TradeAction> queuedActions) {
        return new StrategyEvaluatorContext(null, queuedActions);
    }

    public static StrategyEvaluatorContext ta4jOrQueued(BarSeries series, List<TradeAction> queuedActions) {
        return new StrategyEvaluatorContext(series, queuedActions == null ? List.of() : List.copyOf(queuedActions));
    }
}
