package ch.lueem.tradingbot.bot;

import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.strategy.TradeSignal;

/**
 * Captures the result of one bot cycle including signal, portfolio view and execution result.
 */
public record BotTickResult(
        BotState state,
        MarketSnapshot marketSnapshot,
        PortfolioSnapshot portfolioSnapshot,
        TradeSignal tradeSignal,
        ExecutionResult executionResult
) {
}
