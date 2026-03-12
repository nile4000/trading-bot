package ch.lueem.tradingbot.core.execution;

/**
 * Describes whether an execution request was executed, skipped, or only validated.
 */
public enum ExecutionStatus {
    EXECUTED,
    SKIPPED,
    VALIDATED
}
