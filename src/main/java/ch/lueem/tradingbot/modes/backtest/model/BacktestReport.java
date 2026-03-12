package ch.lueem.tradingbot.modes.backtest.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable summary of one completed backtest run.
 */
public record BacktestReport(
        BacktestMetadata metadata,
        int executedSignalCount,
        int closedTradeCount,
        boolean hasOpenPosition,
        BigDecimal initialCash,
        BigDecimal finalValue,
        BigDecimal totalReturnPercent,
        BigDecimal winRatePercent,
        List<BacktestPositionReport> positions
) {
}
