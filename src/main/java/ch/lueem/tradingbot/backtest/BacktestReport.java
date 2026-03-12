package ch.lueem.tradingbot.backtest;

/**
 * Immutable summary of one completed backtest run.
 */
public record BacktestReport(
        String symbol,
        String timeframe,
        int barCount,
        int tradeCount,
        double initialCash,
        double finalValue,
        double returnPct,
        double winRatePct
) {
}
