package ch.lueem.tradingbot.core.execution;

/**
 * Describes the outcome of one execution attempt.
 */
public record Result(
        Status status,
        boolean executed,
        boolean positionOpenAfterExecution,
        String message
) {
}
