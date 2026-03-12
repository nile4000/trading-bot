package ch.lueem.tradingbot.bot;

/**
 * Describes the current runtime status of a single bot instance.
 */
public enum BotStatus {
    STARTING,
    IDLE,
    RUNNING,
    DEGRADED,
    FAILED,
    STOPPED
}
