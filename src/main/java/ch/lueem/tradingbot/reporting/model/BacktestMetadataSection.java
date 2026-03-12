package ch.lueem.tradingbot.reporting.model;

/**
 * Contains dataset and simulation metadata for the backtest JSON report.
 */
public record BacktestMetadataSection(
        String csvPath,
        String symbol,
        String timeframe,
        int barCount,
        String dataStart,
        String dataEnd,
        String executionModel,
        String positionSizingModel) {
}
