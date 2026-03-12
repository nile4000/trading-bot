package ch.lueem.tradingbot.core.portfolio;

/**
 * Provides the current portfolio state used by the bot runtime.
 */
public interface PortfolioService {

    PortfolioSnapshot getSnapshot(String symbol);
}
