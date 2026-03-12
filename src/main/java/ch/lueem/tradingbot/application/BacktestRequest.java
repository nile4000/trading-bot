package ch.lueem.tradingbot.application;

import java.nio.file.Path;

/**
 * Holds the fixed input parameters for one backtest execution.
 */
public record BacktestRequest(
        Path csvPath,
        String strategyName,
        String symbol,
        String timeframe,
        int shortEma,
        int longEma,
        double initialCash
) {
}
