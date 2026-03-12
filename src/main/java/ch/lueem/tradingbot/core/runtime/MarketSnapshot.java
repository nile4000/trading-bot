package ch.lueem.tradingbot.core.runtime;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Holds the current market data snapshot used for a single runtime cycle.
 */
public record MarketSnapshot(
        String symbol,
        String timeframe,
        OffsetDateTime observedAt,
        BigDecimal lastPrice,
        List<BigDecimal> closePriceHistory,
        int barIndex
) {
}
