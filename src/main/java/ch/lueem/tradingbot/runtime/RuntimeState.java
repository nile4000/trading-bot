package ch.lueem.tradingbot.runtime;

import java.time.OffsetDateTime;

/**
 * Captures the current operational state of one trading runtime.
 */
public record RuntimeState(
        String runtimeId,
        RuntimeStatus status,
        OffsetDateTime lastRunAt,
        OffsetDateTime lastSuccessAt,
        String lastError,
        boolean openPosition
) {
    public static RuntimeState starting(String runtimeId) {
        return new RuntimeState(runtimeId, RuntimeStatus.STARTING, null, null, null, false);
    }
}
