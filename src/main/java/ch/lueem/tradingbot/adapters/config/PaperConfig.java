package ch.lueem.tradingbot.adapters.config;

import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;

/**
 * Holds the configured input for one paper bot runtime.
 */
public record PaperConfig(
        PaperBotConfig bot,
        PaperExecutionConfig execution,
        PaperStrategyConfig strategy,
        BinanceSpotTestnetConfig binance
) {
    public void validate() {
        if (bot == null) {
            throw new IllegalStateException("Missing 'paper.bot' section in application.yml.");
        }
        if (execution == null) {
            throw new IllegalStateException("Missing 'paper.execution' section in application.yml.");
        }
        if (strategy == null) {
            throw new IllegalStateException("Missing 'paper.strategy' section in application.yml.");
        }
        if (binance == null) {
            throw new IllegalStateException("Missing 'paper.binance' section in application.yml.");
        }

        bot.validate();
        execution.validate();
        strategy.validate();
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
                strategy.toStrategyDefinition());
    }
}
