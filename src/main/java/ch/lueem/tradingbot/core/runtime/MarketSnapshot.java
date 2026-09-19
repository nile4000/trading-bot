package ch.lueem.tradingbot.core.runtime;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Holds the current market data snapshot used for a single runtime cycle.
 * The last price marks the portfolio; the execution price is the price used for fills.
 */
public record MarketSnapshot(
        String symbol,
        String timeframe,
        OffsetDateTime observedAt,
        BigDecimal lastPrice,
        BigDecimal executionPrice,
        int barIndex
) {
}
