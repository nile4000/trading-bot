package ch.lueem.tradingbot.core.time;

import java.time.Duration;

/**
 * Parses the supported trading timeframe labels into durations.
 */
public final class Timeframes {

    private Timeframes() {
    }

    public static Duration parse(String timeframe) {
        return switch (timeframe) {
            case "1m" -> Duration.ofMinutes(1);
            case "5m" -> Duration.ofMinutes(5);
            case "15m" -> Duration.ofMinutes(15);
            case "1h" -> Duration.ofHours(1);
            case "4h" -> Duration.ofHours(4);
            case "1d" -> Duration.ofDays(1);
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        };
    }
}
