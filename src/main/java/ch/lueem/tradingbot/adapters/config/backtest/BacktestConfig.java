package ch.lueem.tradingbot.adapters.config.backtest;

import static ch.lueem.tradingbot.adapters.config.ConfigValidation.requireNotBlank;
import static ch.lueem.tradingbot.adapters.config.ConfigValidation.requirePresent;

import java.nio.file.Path;

import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;

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
    public BacktestConfig {
        requirePresent(csvPath, "backtest.csvPath must not be null.");
        requireNotBlank(symbol, "backtest.symbol must not be blank.");
        requireNotBlank(timeframe, "backtest.timeframe must not be blank.");
        requirePresent(strategy, "Missing 'backtest.strategy' section in application.yml.");
        requirePresent(portfolio, "Missing 'backtest.portfolio' section in application.yml.");
    }

    public TradingDefinition toTradingDefinition() {
        return new TradingDefinition(
                "backtest-" + symbol.toLowerCase() + "-" + timeframe,
                "historical",
                BotMode.BACKTEST,
                symbol,
                timeframe,
                strategy);
    }
}
