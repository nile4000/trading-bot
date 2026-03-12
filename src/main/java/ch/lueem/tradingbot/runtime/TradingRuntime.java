package ch.lueem.tradingbot.runtime;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import ch.lueem.tradingbot.bot.market.MarketSnapshot;
import ch.lueem.tradingbot.bot.market.MarketSnapshotProvider;
import ch.lueem.tradingbot.execution.ExecutionRequest;
import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.execution.ExecutionService;
import ch.lueem.tradingbot.portfolio.PortfolioService;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.strategy.signal.SignalContext;
import ch.lueem.tradingbot.strategy.signal.StrategySignalEvaluator;
import ch.lueem.tradingbot.strategy.signal.TradeSignal;

/**
 * Runs one trading cycle: load market state, inspect portfolio, evaluate strategy and delegate execution.
 */
public class TradingRuntime {

    private final TradingDefinition definition;
    private final MarketSnapshotProvider marketSnapshotProvider;
    private final PortfolioService portfolioService;
    private final StrategySignalEvaluator strategySignalEvaluator;
    private final ExecutionService executionService;

    private RuntimeState state;

    public TradingRuntime(
            TradingDefinition definition,
            MarketSnapshotProvider marketSnapshotProvider,
            PortfolioService portfolioService,
            StrategySignalEvaluator strategySignalEvaluator,
            ExecutionService executionService
    ) {
        validateInputs(definition, marketSnapshotProvider, portfolioService, strategySignalEvaluator, executionService);
        this.definition = definition;
        this.marketSnapshotProvider = marketSnapshotProvider;
        this.portfolioService = portfolioService;
        this.strategySignalEvaluator = strategySignalEvaluator;
        this.executionService = executionService;
        this.state = RuntimeState.starting(definition.runtimeId());
    }

    public RuntimeState state() {
        return state;
    }

    public RuntimeCycleResult runCycle() {
        OffsetDateTime runTimestamp = OffsetDateTime.now(ZoneOffset.UTC);

        try {
            markRunning(runTimestamp);

            MarketSnapshot marketSnapshot = marketSnapshotProvider.load(definition);
            validateMarketSnapshot(marketSnapshot);
            PortfolioSnapshot portfolioSnapshotBeforeExecution = portfolioService.getSnapshot(definition.symbol());
            TradeSignal tradeSignal = strategySignalEvaluator.evaluate(toSignalContext(marketSnapshot, portfolioSnapshotBeforeExecution));
            ExecutionResult executionResult = executionService.execute(toExecutionRequest(runTimestamp, marketSnapshot, tradeSignal));
            PortfolioSnapshot portfolioSnapshotAfterExecution = portfolioService.getSnapshot(definition.symbol());

            markSuccess(runTimestamp, portfolioSnapshotAfterExecution.position().open());

            return new RuntimeCycleResult(state, marketSnapshot, portfolioSnapshotAfterExecution, tradeSignal, executionResult);
        } catch (RuntimeException exception) {
            markFailure(runTimestamp, exception);
            throw exception;
        }
    }

    private void markRunning(OffsetDateTime runTimestamp) {
        state = new RuntimeState(
                definition.runtimeId(),
                RuntimeStatus.RUNNING,
                runTimestamp,
                state.lastSuccessAt(),
                null,
                state.openPosition());
    }

    private void markSuccess(OffsetDateTime runTimestamp, boolean openPosition) {
        state = new RuntimeState(
                definition.runtimeId(),
                RuntimeStatus.IDLE,
                runTimestamp,
                runTimestamp,
                null,
                openPosition);
    }

    private void markFailure(OffsetDateTime runTimestamp, RuntimeException exception) {
        state = new RuntimeState(
                definition.runtimeId(),
                RuntimeStatus.FAILED,
                runTimestamp,
                state.lastSuccessAt(),
                exception.getMessage(),
                state.openPosition());
    }

    private void validateMarketSnapshot(MarketSnapshot marketSnapshot) {
        if (marketSnapshot == null) {
            throw new IllegalStateException("marketSnapshotProvider returned null for runtime " + definition.runtimeId());
        }
        if (!definition.symbol().equals(marketSnapshot.symbol())) {
            throw new IllegalStateException("Market snapshot symbol does not match runtime definition: "
                    + marketSnapshot.symbol());
        }
        if (!definition.timeframe().equals(marketSnapshot.timeframe())) {
            throw new IllegalStateException("Market snapshot timeframe does not match runtime definition: "
                    + marketSnapshot.timeframe());
        }
        if (marketSnapshot.lastPrice() == null || marketSnapshot.lastPrice().signum() <= 0) {
            throw new IllegalStateException("Market snapshot price must be greater than zero for runtime " + definition.runtimeId());
        }
        if (marketSnapshot.observedAt() == null) {
            throw new IllegalStateException("Market snapshot observedAt must not be null for runtime " + definition.runtimeId());
        }
    }

    private SignalContext toSignalContext(MarketSnapshot marketSnapshot, PortfolioSnapshot portfolioSnapshot) {
        return new SignalContext(
                definition.symbol(),
                definition.timeframe(),
                marketSnapshot.observedAt(),
                marketSnapshot.lastPrice(),
                portfolioSnapshot.position().open(),
                marketSnapshot.closePriceHistory());
    }

    private ExecutionRequest toExecutionRequest(
            OffsetDateTime runTimestamp,
            MarketSnapshot marketSnapshot,
            TradeSignal tradeSignal) {
        return new ExecutionRequest(
                definition.runtimeId(),
                definition.symbol(),
                definition.timeframe(),
                tradeSignal,
                runTimestamp,
                marketSnapshot.lastPrice());
    }

    private void validateInputs(
            TradingDefinition definition,
            MarketSnapshotProvider marketSnapshotProvider,
            PortfolioService portfolioService,
            StrategySignalEvaluator strategySignalEvaluator,
            ExecutionService executionService
    ) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null.");
        }
        if (marketSnapshotProvider == null) {
            throw new IllegalArgumentException("marketSnapshotProvider must not be null.");
        }
        if (portfolioService == null) {
            throw new IllegalArgumentException("portfolioService must not be null.");
        }
        if (strategySignalEvaluator == null) {
            throw new IllegalArgumentException("strategySignalEvaluator must not be null.");
        }
        if (executionService == null) {
            throw new IllegalArgumentException("executionService must not be null.");
        }
    }
}
