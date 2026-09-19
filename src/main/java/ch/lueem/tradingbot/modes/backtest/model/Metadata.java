package ch.lueem.tradingbot.modes.backtest.model;

import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;

/**
 * Describes the simulation context and assumptions of one backtest run.
 *
 * @param mode the shared execution mode, using BACKTEST for historical simulation
 * @param executionModel the fill timing assumption, e.g. signal evaluation at close and execution at the next open
 * @param positionSizingModel the position sizing rule used for each entry
 */
public record Metadata(
        BotMode mode,
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
