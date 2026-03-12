package ch.lueem.tradingbot.reporting.model;

import java.util.List;

import ch.lueem.tradingbot.backtest.model.BacktestPositionReport;

/**
 * Represents the versioned top-level JSON document emitted for one backtest run.
 */
public record BacktestReportDocument(
        String timestampUtc,
        String reportVersion,
        BacktestMetadataSection metadata,
        BacktestStrategySection strategy,
        BacktestPerformanceSection performance,
        List<BacktestPositionReport> positions,
        List<String> notes) {
}
