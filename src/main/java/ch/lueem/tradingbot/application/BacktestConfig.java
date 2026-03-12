package ch.lueem.tradingbot.application;

import java.nio.file.Path;

/**
 * Holds the configured backtest input for one application run.
 */
public record BacktestConfig(
        Path csvPath,
        String symbol,
        String timeframe,
        StrategyConfig strategy,
        PortfolioConfig portfolio
) {
    public BacktestRequest toRequest() {
        validate();
        return new BacktestRequest(
                csvPath,
                strategy.name(),
                symbol,
                timeframe,
                strategy.shortEma(),
                strategy.longEma(),
                portfolio.initialCash());
    }

    private void validate() {
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
