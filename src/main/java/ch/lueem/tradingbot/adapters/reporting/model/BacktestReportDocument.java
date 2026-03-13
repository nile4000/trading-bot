package ch.lueem.tradingbot.adapters.reporting.model;

import java.math.BigDecimal;
import java.util.List;

import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;

/**
 * Represents the versioned top-level JSON document emitted for one backtest run.
 */
public record BacktestReportDocument(
        String timestampUtc,
        String reportVersion,
        Metadata metadata,
        Strategy strategy,
        Performance performance,
        List<String> notes) {

    public record Metadata(
            BotMode mode,
            String csvPath,
            String symbol,
            String timeframe,
            int barCount,
            String dataStart,
            String dataEnd,
            String executionModel,
            String positionSizingModel) {
    }

    public record Strategy(
            String name,
            StrategyParameters parameters) {
    }

    public record Performance(
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
            BigDecimal exposurePercent) {
    }
}
