package ch.lueem.tradingbot.reporting.model;

import java.math.BigDecimal;

/**
 * Contains summary performance values for the backtest JSON report.
 */
public record BacktestPerformanceSection(
        int executedSignalCount,
        int closedTradeCount,
        boolean hasOpenPosition,
        BigDecimal initialCash,
        BigDecimal finalValue,
        BigDecimal totalReturnPercent,
        BigDecimal winRatePercent) {
}
