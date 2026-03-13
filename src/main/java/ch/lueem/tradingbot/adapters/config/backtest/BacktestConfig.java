package ch.lueem.tradingbot.adapters.config.backtest;

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
