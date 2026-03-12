package ch.lueem.tradingbot.runtime;

/**
 * Describes the current runtime status of a single trading process.
 */
public enum RuntimeStatus {
    STARTING,
    IDLE,
    RUNNING,
    DEGRADED,
    FAILED,
    STOPPED
}
