package ch.lueem.tradingbot.adapters.reporting.model;

import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;

/**
 * Describes the strategy configuration used for the backtest JSON report.
 */
public record BacktestStrategySection(
        String name,
        StrategyParameters parameters) {
}
