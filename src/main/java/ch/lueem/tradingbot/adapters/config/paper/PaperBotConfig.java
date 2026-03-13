package ch.lueem.tradingbot.adapters.config.paper;

import static ch.lueem.tradingbot.adapters.config.ConfigValidation.requireNotBlank;

/**
 * Holds the stable identity and market scope of one configured paper bot.
 */
public record PaperBotConfig(
        String botId,
        String botVersion,
        String symbol,
        String timeframe
) {
    public PaperBotConfig {
        requireNotBlank(botId, "paper.bot.botId must not be blank.");
        requireNotBlank(botVersion, "paper.bot.botVersion must not be blank.");
        requireNotBlank(symbol, "paper.bot.symbol must not be blank.");
        requireNotBlank(timeframe, "paper.bot.timeframe must not be blank.");
    }
}
