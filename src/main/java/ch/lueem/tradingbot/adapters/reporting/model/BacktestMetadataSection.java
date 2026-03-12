package ch.lueem.tradingbot.adapters.reporting.model;

import ch.lueem.tradingbot.core.runtime.BotMode;

/**
 * Contains dataset and simulation metadata for the backtest JSON report.
 */
public record BacktestMetadataSection(
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
