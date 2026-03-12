package ch.lueem.tradingbot.core.strategy.action;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.ta4j.core.BarSeries;

/**
 * Holds the minimal market and position context required for one strategy evaluation.
 */
public record ActionContext(
        String symbol,
        String timeframe,
        OffsetDateTime observedAt,
        BigDecimal lastPrice,
        boolean openPosition,
        List<BigDecimal> closePriceHistory,
        int barIndex,
        BarSeries barSeries
) {
}
