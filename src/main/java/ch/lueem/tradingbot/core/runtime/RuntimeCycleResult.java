package ch.lueem.tradingbot.core.runtime;

import ch.lueem.tradingbot.core.execution.ExecutionResult;
import ch.lueem.tradingbot.core.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;

/**
 * Captures the result of one runtime cycle including action, portfolio and execution outcome.
 */
public record RuntimeCycleResult(
        MarketSnapshot marketSnapshot,
        PortfolioSnapshot portfolioSnapshot,
        TradeAction action,
        ExecutionResult executionResult
) {
}
