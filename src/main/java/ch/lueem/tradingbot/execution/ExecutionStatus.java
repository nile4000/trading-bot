package ch.lueem.tradingbot.execution;

/**
 * Describes whether an execution request was executed, skipped, or only validated.
 */
public enum ExecutionStatus {
    EXECUTED,
    SKIPPED,
    VALIDATED
}
