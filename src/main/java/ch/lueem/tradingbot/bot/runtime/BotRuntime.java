package ch.lueem.tradingbot.bot.runtime;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import ch.lueem.tradingbot.bot.market.MarketSnapshot;
import ch.lueem.tradingbot.bot.market.MarketSnapshotProvider;
import ch.lueem.tradingbot.bot.model.BotDefinition;
import ch.lueem.tradingbot.bot.model.BotState;
import ch.lueem.tradingbot.bot.model.BotStatus;
import ch.lueem.tradingbot.bot.model.BotTickResult;
import ch.lueem.tradingbot.execution.ExecutionRequest;
import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.execution.ExecutionService;
import ch.lueem.tradingbot.portfolio.PortfolioService;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.strategy.signal.SignalContext;
import ch.lueem.tradingbot.strategy.signal.StrategySignalEvaluator;
import ch.lueem.tradingbot.strategy.signal.TradeSignal;

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
            markRunning(runTimestamp);

            MarketSnapshot marketSnapshot = marketSnapshotProvider.load(definition);
            PortfolioSnapshot portfolioSnapshot = portfolioService.getSnapshot(definition.symbol());
            TradeSignal tradeSignal = strategySignalEvaluator.evaluate(toSignalContext(marketSnapshot, portfolioSnapshot));
            ExecutionResult executionResult = executionService.execute(toExecutionRequest(runTimestamp, marketSnapshot, tradeSignal));

            markSuccess(runTimestamp, executionResult.positionOpenAfterExecution());

            return new BotTickResult(state, marketSnapshot, portfolioSnapshot, tradeSignal, executionResult);
        } catch (RuntimeException exception) {
            markFailure(runTimestamp, exception);
            throw exception;
        }
    }

    private void markRunning(OffsetDateTime runTimestamp) {
        state = new BotState(
                definition.botId(),
                BotStatus.RUNNING,
                runTimestamp,
                state.lastSuccessAt(),
                null,
                state.openPosition());
    }

    private void markSuccess(OffsetDateTime runTimestamp, boolean openPosition) {
        state = new BotState(
                definition.botId(),
                BotStatus.IDLE,
                runTimestamp,
                runTimestamp,
                null,
                openPosition);
    }

    private void markFailure(OffsetDateTime runTimestamp, RuntimeException exception) {
        state = new BotState(
                definition.botId(),
                BotStatus.FAILED,
                runTimestamp,
                state.lastSuccessAt(),
                exception.getMessage(),
                state.openPosition());
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
