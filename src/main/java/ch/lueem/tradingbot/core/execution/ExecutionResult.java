package ch.lueem.tradingbot.core.execution;

/**
 * Describes the outcome of one execution attempt.
 */
public record ExecutionResult(
        ExecutionStatus status,
        boolean executed,
        boolean positionOpenAfterExecution,
        String message
) {
}
