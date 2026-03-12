package ch.lueem.tradingbot.bot.model;

import java.time.OffsetDateTime;

/**
 * Captures the current operational state of a bot runtime.
 */
public record BotState(
        String botId,
        BotStatus status,
        OffsetDateTime lastRunAt,
        OffsetDateTime lastSuccessAt,
        String lastError,
        boolean openPosition
) {
    public static BotState starting(String botId) {
        return new BotState(botId, BotStatus.STARTING, null, null, null, false);
    }
}
