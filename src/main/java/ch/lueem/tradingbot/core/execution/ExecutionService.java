package ch.lueem.tradingbot.core.execution;

/**
 * Executes a trade decision for a specific runtime mode such as backtest, paper or live.
 */
public interface ExecutionService {

    ExecutionResult execute(ExecutionRequest request);
}
