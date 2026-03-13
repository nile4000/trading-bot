package ch.lueem.tradingbot.core.runtime;

import ch.lueem.tradingbot.core.execution.ExecutionService;
import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.execution.Result;
import ch.lueem.tradingbot.core.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.core.portfolio.PortfolioService;
import ch.lueem.tradingbot.core.strategy.action.ActionContext;
import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;

/**
 * Orchestrates one cycle of market observation, strategy evaluation and order
 * execution.
 */
public class TradingRuntime {

    private final TradingDefinition definition;
    private final MarketSnapshotProvider marketSnapshotProvider;
    private final PortfolioService portfolioService;
    private final StrategyActionEvaluator evaluator;
    private final ExecutionService executionService;

    public TradingRuntime(
            TradingDefinition definition,
            MarketSnapshotProvider marketSnapshotProvider,
            PortfolioService portfolioService,
            StrategyActionEvaluator evaluator,
            ExecutionService executionService) {
        this.definition = definition;
        this.marketSnapshotProvider = marketSnapshotProvider;
        this.portfolioService = portfolioService;
        this.evaluator = evaluator;
        this.executionService = executionService;
    }

    public RuntimeCycleResult cycle() {
        MarketSnapshot snapshot = marketSnapshotProvider.load(definition);
        validateSnapshot(snapshot);
        PortfolioSnapshot portfolioSnapshotBeforeExecution = portfolioService.getSnapshot(definition.symbol());

        TradeAction action = evaluator.evaluate(new ActionContext(
                definition.symbol(),
                definition.timeframe(),
                snapshot.observedAt(),
                snapshot.lastPrice(),
                portfolioSnapshotBeforeExecution.position().open(),
                snapshot.closePriceHistory(),
                snapshot.barIndex(),
                null));

        Result executionResult = executionService.execute(new Request(
                definition.runtimeId(),
                definition.symbol(),
                definition.timeframe(),
                action,
                snapshot.observedAt(),
                snapshot.lastPrice()));
        PortfolioSnapshot portfolioSnapshotAfterExecution = portfolioService.getSnapshot(definition.symbol());

        return new RuntimeCycleResult(
                snapshot,
                portfolioSnapshotAfterExecution,
                action,
                executionResult);
    }

    public TradingDefinition definition() {
        return definition;
    }

    private void validateSnapshot(MarketSnapshot snapshot) {
        if (!definition.symbol().equals(snapshot.symbol())) {
            throw new IllegalStateException(
                    "Market snapshot symbol does not match runtime symbol: " + snapshot.symbol());
        }
        if (!definition.timeframe().equals(snapshot.timeframe())) {
            throw new IllegalStateException(
                    "Market snapshot timeframe does not match runtime timeframe: " + snapshot.timeframe());
        }
    }
}
