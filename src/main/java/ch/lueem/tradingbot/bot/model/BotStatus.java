package ch.lueem.tradingbot.bot.model;

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
