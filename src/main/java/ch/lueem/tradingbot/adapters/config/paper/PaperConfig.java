package ch.lueem.tradingbot.adapters.config.paper;

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
