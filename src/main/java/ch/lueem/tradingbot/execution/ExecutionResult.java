package ch.lueem.tradingbot.execution;

/**
 * Describes the outcome of one execution attempt.
 */
public record ExecutionResult(
        boolean executed,
        boolean positionOpenAfterExecution,
        String message
) {
}
