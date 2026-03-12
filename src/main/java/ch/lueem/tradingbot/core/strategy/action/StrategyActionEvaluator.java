package ch.lueem.tradingbot.core.strategy.action;

/**
 * Evaluates a strategy decision for the current market and position context.
 */
public interface StrategyActionEvaluator {

    TradeAction evaluate(ActionContext context);
}
