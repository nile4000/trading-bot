package ch.lueem.tradingbot.adapters.config.paper;

/**
 * Holds the stable identity and market scope of one configured paper bot.
 */
public record PaperBotConfig(
        String botId,
        String botVersion,
        String symbol,
        String timeframe
) {
}
