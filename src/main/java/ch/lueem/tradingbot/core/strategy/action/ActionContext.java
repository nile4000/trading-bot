package ch.lueem.tradingbot.core.strategy.action;

/**
 * Holds the minimal market and position context required for one strategy evaluation.
 */
public record ActionContext(
        boolean openPosition,
        int barIndex
) {
}
