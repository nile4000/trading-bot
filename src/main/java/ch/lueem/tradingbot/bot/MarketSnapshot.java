package ch.lueem.tradingbot.bot;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Holds the current market data snapshot used for a single bot cycle.
 */
public record MarketSnapshot(
        String symbol,
        String timeframe,
        OffsetDateTime observedAt,
        BigDecimal lastPrice
) {
}
