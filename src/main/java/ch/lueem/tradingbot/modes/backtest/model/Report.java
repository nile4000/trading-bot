package ch.lueem.tradingbot.modes.backtest.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable summary of one completed backtest run.
 */
public record Report(
        Metadata metadata,
        int closedTradeCount,
        BigDecimal initialCash,
        BigDecimal finalValue,
        BigDecimal totalReturnPercent,
        BigDecimal buyAndHoldReturnPercent,
        BigDecimal maxDrawdownPercent,
        BigDecimal profitFactor,
        BigDecimal winRatePercent,
        BigDecimal averageWinningTrade,
        BigDecimal averageLosingTrade,
        BigDecimal timeInMarketDays,
        BigDecimal exposurePercent,
        boolean hasOpenPosition,
        List<Position> positions
) {
    /**
     * Describes one simulated position from entry to exit or current
     * mark-to-market state.
     */
    public record Position(
            int positionNumber,
            String status,
            String entryTime,
            BigDecimal entryPrice,
            String exitTime,
            BigDecimal exitPrice,
            BigDecimal quantity,
            BigDecimal profitLoss,
            BigDecimal profitLossPercent) {
    }
}
