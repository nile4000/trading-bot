package ch.lueem.tradingbot.backtest;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable summary of one completed backtest run.
 */
public record BacktestReport(
        BacktestMetadata metadata,
        int signalCount,
        int tradeCount,
        boolean openPosition,
        BigDecimal initialCash,
        BigDecimal finalValue,
        BigDecimal returnPct,
        BigDecimal winRatePct,
        List<BacktestPositionReport> positions
) {
}
