package ch.lueem.tradingbot.backtest;

import ch.lueem.tradingbot.strategy.StrategyDefinition;

/**
 * Describes the simulation context and assumptions of one backtest run.
 */
public record BacktestMetadata(
        String symbol,
        String timeframe,
        int barCount,
        String dataStart,
        String dataEnd,
        String executionModel,
        String positionSizingModel,
        StrategyDefinition strategy
) {
}
