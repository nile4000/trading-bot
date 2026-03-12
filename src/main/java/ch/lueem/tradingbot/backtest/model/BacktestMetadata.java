package ch.lueem.tradingbot.backtest.model;

import ch.lueem.tradingbot.bot.model.BotMode;
import ch.lueem.tradingbot.strategy.definition.StrategyDefinition;

/**
 * Describes the simulation context and assumptions of one backtest run.
 *
 * @param mode the shared execution mode, using BACKTEST for historical simulation
 * @param executionModel the fill timing assumption, e.g. action execution on the action-bar close
 * @param positionSizingModel the position sizing rule, e.g. full cash allocation per entry
 */
public record BacktestMetadata(
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
