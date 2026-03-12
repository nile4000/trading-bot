package ch.lueem.tradingbot.runtime;

import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.strategy.action.TradeAction;

/**
 * Captures the result of one runtime cycle including action, portfolio and execution outcome.
 */
public record RuntimeCycleResult(
        RuntimeState state,
        MarketSnapshot marketSnapshot,
        PortfolioSnapshot portfolioSnapshot,
        TradeAction action,
        ExecutionResult executionResult
) {
}
