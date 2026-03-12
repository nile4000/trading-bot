package ch.lueem.tradingbot.bot;

/**
 * Holds the stable identity and market scope of one bot runtime.
 */
public record BotDefinition(
        String botId,
        String botVersion,
        BotMode mode,
        String symbol,
        String timeframe,
        String strategyName
) {
}
