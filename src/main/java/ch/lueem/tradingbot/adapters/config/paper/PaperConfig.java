package ch.lueem.tradingbot.adapters.config.paper;

import static ch.lueem.tradingbot.adapters.config.ConfigValidation.requirePresent;

import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;

/**
 * Holds the configured input for one paper bot runtime.
 */
public record PaperConfig(
        PaperBotConfig bot,
        PaperExecutionConfig execution,
        PaperStrategyConfig strategy,
        BinanceConfig binance
) {
    public PaperConfig {
        requirePresent(bot, "Missing 'paper.bot' section in application.yml.");
        requirePresent(execution, "Missing 'paper.execution' section in application.yml.");
        requirePresent(strategy, "Missing 'paper.strategy' section in application.yml.");
        requirePresent(binance, "Missing 'paper.binance' section in application.yml.");
    }

    public TradingDefinition toTradingDefinition() {
        return new TradingDefinition(
                bot.botId(),
                bot.botVersion(),
                BotMode.PAPER,
                bot.symbol(),
                bot.timeframe(),
                strategy.toStrategyDefinition());
    }
}
