package ch.lueem.tradingbot.reporting.model;

import ch.lueem.tradingbot.bot.model.BotMode;

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
