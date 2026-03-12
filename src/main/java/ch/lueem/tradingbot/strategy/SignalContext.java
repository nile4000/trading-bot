package ch.lueem.tradingbot.strategy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Holds the minimal market and position context required for one strategy evaluation.
 */
public record SignalContext(
        String symbol,
        String timeframe,
        OffsetDateTime observedAt,
        BigDecimal lastPrice,
        boolean openPosition
) {
}
