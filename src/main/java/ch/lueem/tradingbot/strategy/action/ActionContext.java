package ch.lueem.tradingbot.strategy.action;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Holds the minimal market and position context required for one strategy evaluation.
 */
public record ActionContext(
        String symbol,
        String timeframe,
        OffsetDateTime observedAt,
        BigDecimal lastPrice,
        boolean openPosition,
        List<BigDecimal> closePriceHistory
) {
}
