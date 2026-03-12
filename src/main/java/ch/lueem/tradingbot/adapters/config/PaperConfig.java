package ch.lueem.tradingbot.adapters.config;

import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;

/**
 * Holds the configured input for one paper bot runtime.
 */
public record PaperConfig(
        PaperBotConfig bot,
        PaperExecutionConfig execution,
        PaperActionSourceConfig actionSource,
        BinanceSpotTestnetConfig binance
) {
    public void validate() {
        if (bot == null) {
            throw new IllegalStateException("Missing 'paper.bot' section in application.yml.");
        }
        if (execution == null) {
            throw new IllegalStateException("Missing 'paper.execution' section in application.yml.");
        }
        if (actionSource == null) {
            throw new IllegalStateException("Missing 'paper.actionSource' section in application.yml.");
        }
        if (binance == null) {
            throw new IllegalStateException("Missing 'paper.binance' section in application.yml.");
        }

        bot.validate();
        execution.validate();
        actionSource.validate();
        binance.validate();
    }

    public TradingDefinition toTradingDefinition() {
        validate();
        return new TradingDefinition(
                bot.botId(),
                bot.botVersion(),
                BotMode.PAPER,
                bot.symbol(),
                bot.timeframe(),
                new StrategyDefinition(actionSource.strategyName(), null));
    }
}
