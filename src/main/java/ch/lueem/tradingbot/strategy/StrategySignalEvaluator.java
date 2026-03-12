package ch.lueem.tradingbot.strategy;

/**
 * Evaluates a strategy decision for the current market and position context.
 */
public interface StrategySignalEvaluator {

    TradeSignal evaluate(SignalContext context);
}
