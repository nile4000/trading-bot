package ch.lueem.tradingbot.bot;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import ch.lueem.tradingbot.execution.ExecutionRequest;
import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.execution.ExecutionService;
import ch.lueem.tradingbot.portfolio.PortfolioService;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.strategy.SignalContext;
import ch.lueem.tradingbot.strategy.StrategySignalEvaluator;
import ch.lueem.tradingbot.strategy.TradeSignal;

/**
 * Runs one bot cycle: load market state, inspect portfolio, evaluate strategy and delegate execution.
 */
public class BotRuntime {

    private final BotDefinition definition;
    private final MarketSnapshotProvider marketSnapshotProvider;
    private final PortfolioService portfolioService;
    private final StrategySignalEvaluator strategySignalEvaluator;
    private final ExecutionService executionService;

    private BotState state;

    public BotRuntime(
            BotDefinition definition,
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
        this.state = BotState.starting(definition.botId());
    }

    public BotState state() {
        return state;
    }

    public BotTickResult runCycle() {
        OffsetDateTime runTimestamp = OffsetDateTime.now(ZoneOffset.UTC);

        try {
            state = new BotState(definition.botId(), BotStatus.RUNNING, runTimestamp, state.lastSuccessAt(), null, state.openPosition());

            MarketSnapshot marketSnapshot = marketSnapshotProvider.load(definition);
            PortfolioSnapshot portfolioSnapshot = portfolioService.getSnapshot(definition.symbol());
            TradeSignal tradeSignal = strategySignalEvaluator.evaluate(toSignalContext(marketSnapshot, portfolioSnapshot));
            ExecutionResult executionResult = executionService.execute(toExecutionRequest(runTimestamp, marketSnapshot, tradeSignal));

            state = new BotState(
                    definition.botId(),
                    BotStatus.IDLE,
                    runTimestamp,
                    runTimestamp,
                    null,
                    executionResult.positionOpenAfterExecution());

            return new BotTickResult(state, marketSnapshot, portfolioSnapshot, tradeSignal, executionResult);
        } catch (RuntimeException exception) {
            state = new BotState(
                    definition.botId(),
                    BotStatus.FAILED,
                    runTimestamp,
                    state.lastSuccessAt(),
                    exception.getMessage(),
                    state.openPosition());
            throw exception;
        }
    }

    private SignalContext toSignalContext(MarketSnapshot marketSnapshot, PortfolioSnapshot portfolioSnapshot) {
        return new SignalContext(
                definition.symbol(),
                definition.timeframe(),
                marketSnapshot.observedAt(),
                marketSnapshot.lastPrice(),
                portfolioSnapshot.position().open());
    }

    private ExecutionRequest toExecutionRequest(
            OffsetDateTime runTimestamp,
            MarketSnapshot marketSnapshot,
            TradeSignal tradeSignal) {
        return new ExecutionRequest(
                definition.botId(),
                definition.symbol(),
                definition.timeframe(),
                tradeSignal,
                runTimestamp,
                marketSnapshot.lastPrice());
    }

    private void validateInputs(
            BotDefinition definition,
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
