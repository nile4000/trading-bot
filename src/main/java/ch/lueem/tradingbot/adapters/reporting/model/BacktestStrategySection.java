package ch.lueem.tradingbot.adapters.reporting.model;

/**
 * Describes the strategy configuration used for the backtest JSON report.
 */
public record BacktestStrategySection(
        String name,
        int shortEma,
        int longEma) {
}
