package ch.lueem.tradingbot.application;

import java.nio.file.Path;

import ch.lueem.tradingbot.strategy.definition.StrategyDefinition;

/**
 * Holds the fixed input parameters for one backtest execution.
 */
public record BacktestRequest(
        Path csvPath,
        String symbol,
        String timeframe,
        StrategyDefinition strategy,
        double initialCash
) {
    public String strategyName() {
        return strategy.name();
    }

    public int shortEma() {
        return strategy.parameters().shortEma();
    }

    public int longEma() {
        return strategy.parameters().longEma();
    }
}
