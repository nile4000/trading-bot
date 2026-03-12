package ch.lueem.tradingbot.application;

import ch.lueem.tradingbot.bot.model.BotMode;
import ch.lueem.tradingbot.runtime.TradingDefinition;
import ch.lueem.tradingbot.strategy.definition.StrategyDefinition;

/**
 * Holds the configured input for one paper bot runtime.
 */
public record PaperConfig(
        PaperBotConfig bot,
        PaperExecutionConfig execution,
        PaperSignalSourceConfig signalSource,
        BinanceSpotTestnetConfig binance
) {
    public void validate() {
        if (bot == null) {
            throw new IllegalStateException("Missing 'paper.bot' section in application.yml.");
        }
        if (execution == null) {
            throw new IllegalStateException("Missing 'paper.execution' section in application.yml.");
        }
        if (signalSource == null) {
            throw new IllegalStateException("Missing 'paper.signalSource' section in application.yml.");
        }
        if (binance == null) {
            throw new IllegalStateException("Missing 'paper.binance' section in application.yml.");
        }

        bot.validate();
        execution.validate();
        signalSource.validate();
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
                new StrategyDefinition(signalSource.strategyName(), null));
    }
}
