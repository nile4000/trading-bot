package ch.lueem.tradingbot.adapters.config.backtest;

import java.math.BigDecimal;

/**
 * Holds the configured portfolio settings for the backtest.
 */
public record PortfolioConfig(BigDecimal initialCash) {
}
