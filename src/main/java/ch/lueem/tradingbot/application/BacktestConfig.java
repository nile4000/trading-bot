package ch.lueem.tradingbot.application;

import java.nio.file.Path;

import ch.lueem.tradingbot.bot.model.BotMode;
import ch.lueem.tradingbot.runtime.TradingDefinition;
import ch.lueem.tradingbot.strategy.definition.StrategyDefinition;

/**
 * Holds the configured backtest input for one application run.
 */
public record BacktestConfig(
        Path csvPath,
        String symbol,
        String timeframe,
        StrategyDefinition strategy,
        PortfolioConfig portfolio
) {
    public TradingDefinition toTradingDefinition() {
        validate();
        return new TradingDefinition(
                "backtest-" + symbol.toLowerCase() + "-" + timeframe,
                "historical",
                BotMode.BACKTEST,
                symbol,
                timeframe,
                strategy);
    }

    public void validate() {
        if (csvPath == null) {
            throw new IllegalStateException("backtest.csvPath must not be null.");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalStateException("backtest.symbol must not be blank.");
        }
        if (timeframe == null || timeframe.isBlank()) {
            throw new IllegalStateException("backtest.timeframe must not be blank.");
        }
        if (strategy == null) {
            throw new IllegalStateException("Missing 'backtest.strategy' section in application.yml.");
        }
        if (portfolio == null) {
            throw new IllegalStateException("Missing 'backtest.portfolio' section in application.yml.");
        }
    }
}
