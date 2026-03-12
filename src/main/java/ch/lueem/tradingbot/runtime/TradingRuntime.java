package ch.lueem.tradingbot.runtime;

import ch.lueem.tradingbot.execution.ExecutionRequest;
import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.execution.ExecutionService;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.portfolio.PortfolioService;
import ch.lueem.tradingbot.strategy.action.ActionContext;
import ch.lueem.tradingbot.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.strategy.action.TradeAction;

/**
 * Orchestrates one cycle of market observation, strategy evaluation and order execution.
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
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null.");
        }
        if (marketSnapshotProvider == null) {
            throw new IllegalArgumentException("marketSnapshotProvider must not be null.");
        }
        if (portfolioService == null) {
            throw new IllegalArgumentException("portfolioService must not be null.");
        }
        if (evaluator == null) {
            throw new IllegalArgumentException("evaluator must not be null.");
        }
        if (executionService == null) {
            throw new IllegalArgumentException("executionService must not be null.");
        }
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
                snapshot.closePriceHistory()));

        ExecutionResult executionResult = executionService.execute(new ExecutionRequest(
                definition.runtimeId(),
                definition.symbol(),
                definition.timeframe(),
                action,
                snapshot.observedAt(),
                snapshot.lastPrice()));
        PortfolioSnapshot portfolioSnapshotAfterExecution = portfolioService.getSnapshot(definition.symbol());

        return new RuntimeCycleResult(
                new RuntimeState(
                        definition.runtimeId(),
                        RuntimeStatus.RUNNING,
                        snapshot.observedAt(),
                        executionResult.status() == null ? null : snapshot.observedAt(),
                        null,
                        portfolioSnapshotAfterExecution.position().open()),
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
            throw new IllegalStateException("Market snapshot symbol does not match runtime symbol: " + snapshot.symbol());
        }
        if (!definition.timeframe().equals(snapshot.timeframe())) {
            throw new IllegalStateException("Market snapshot timeframe does not match runtime timeframe: " + snapshot.timeframe());
        }
    }
}
