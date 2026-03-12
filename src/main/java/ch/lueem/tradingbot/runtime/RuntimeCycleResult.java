package ch.lueem.tradingbot.runtime;

import ch.lueem.tradingbot.bot.market.MarketSnapshot;
import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.strategy.signal.TradeSignal;

/**
 * Captures the result of one runtime cycle including signal, portfolio and execution outcome.
 */
public record RuntimeCycleResult(
        RuntimeState state,
        MarketSnapshot marketSnapshot,
        PortfolioSnapshot portfolioSnapshot,
        TradeSignal tradeSignal,
        ExecutionResult executionResult
) {
}
