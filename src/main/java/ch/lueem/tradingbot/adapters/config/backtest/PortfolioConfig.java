package ch.lueem.tradingbot.adapters.config.backtest;

import static ch.lueem.tradingbot.adapters.config.ConfigValidation.requirePositive;

/**
 * Holds the configured portfolio settings for the backtest.
 */
public record PortfolioConfig(double initialCash) {
    public PortfolioConfig {
        requirePositive(initialCash, "backtest.portfolio.initialCash must be greater than zero.");
    }
}
