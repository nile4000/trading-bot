package ch.lueem.tradingbot.application;

/**
 * Holds the stable identity and market scope of one configured paper bot.
 */
public record PaperBotConfig(
        String botId,
        String botVersion,
        String symbol,
        String timeframe
) {
    public void validate() {
        if (botId == null || botId.isBlank()) {
            throw new IllegalStateException("paper.bot.botId must not be blank.");
        }
        if (botVersion == null || botVersion.isBlank()) {
            throw new IllegalStateException("paper.bot.botVersion must not be blank.");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalStateException("paper.bot.symbol must not be blank.");
        }
        if (timeframe == null || timeframe.isBlank()) {
            throw new IllegalStateException("paper.bot.timeframe must not be blank.");
        }
    }
}
