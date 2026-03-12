package ch.lueem.tradingbot.backtest;

import java.math.BigDecimal;

/**
 * Describes one simulated position from entry to exit or current mark-to-market state.
 */
public record BacktestPositionReport(
        int positionNumber,
        String status,
        String entryTime,
        BigDecimal entryPrice,
        String exitTime,
        BigDecimal exitPrice,
        BigDecimal quantity,
        BigDecimal pnl,
        BigDecimal pnlPct
) {
}
