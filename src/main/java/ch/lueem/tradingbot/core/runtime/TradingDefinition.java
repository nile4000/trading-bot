package ch.lueem.tradingbot.core.runtime;

import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;

/**
 * Holds the stable identity, market scope and strategy for one runtime.
 */
public record TradingDefinition(
        String runtimeId,
        String runtimeVersion,
        BotMode mode,
        String symbol,
        String timeframe,
        StrategyDefinition strategy
) {
}
