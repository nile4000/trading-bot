package ch.lueem.tradingbot.modes.backtest;

import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.adapters.market.CsvBarSeriesLoader;
import ch.lueem.tradingbot.core.time.Timeframes;
import ch.lueem.tradingbot.modes.backtest.model.Report;
import ch.lueem.tradingbot.adapters.market.CsvMarketSnapshotProvider;
import ch.lueem.tradingbot.adapters.execution.simulated.SimulatedExecutionService;
import ch.lueem.tradingbot.adapters.portfolio.SimulatedPortfolioService;
import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.execution.Result;
import ch.lueem.tradingbot.core.execution.Status;
import ch.lueem.tradingbot.core.runtime.RuntimeCycleResult;
import ch.lueem.tradingbot.core.strategy.StrategyEvaluatorContext;
import ch.lueem.tradingbot.core.strategy.StrategyEvaluatorFactory;
import ch.lueem.tradingbot.core.strategy.action.ActionContext;
import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.ta4j.core.BarSeries;
import org.ta4j.core.analysis.cost.LinearTransactionCostModel;

/**
 * Coordinates CSV loading, strategy creation and result calculation for one
 * backtest run.
 */
@Singleton
public class Runner {

    private final CsvBarSeriesLoader csvBarSeriesLoader;
    private final ReportGenerator reportGenerator;
    private final StrategyEvaluatorFactory strategyFactory;

    public Runner() {
        this(new CsvBarSeriesLoader(),
                new ReportGenerator(),
                new StrategyEvaluatorFactory());
    }

    @Inject
    public Runner(
            CsvBarSeriesLoader csvBarSeriesLoader,
            ReportGenerator reportGenerator,
            StrategyEvaluatorFactory strategyFactory) {
        this.csvBarSeriesLoader = csvBarSeriesLoader;
        this.reportGenerator = reportGenerator;
        this.strategyFactory = strategyFactory;
    }

    public Report backtest(BacktestConfig config) {
        var series = createBarSeries(config);
        var marketSnapshotProvider = createMarketSnapshotProvider(config, series);
        var portfolioService = createPortfolioService(config);
        var evaluator = createStrategyEvaluator(config, series);
        var executionService = new SimulatedExecutionService(
                portfolioService,
                config.orderQuantity(),
                config.slippageRate(),
                new LinearTransactionCostModel(config.executionFeeRate().doubleValue()));

        return reportGenerator.assemble(
                config,
                runHistoricalCycles(config, marketSnapshotProvider, portfolioService, evaluator,
                        executionService));
    }

    private BarSeries createBarSeries(BacktestConfig config) {
        return csvBarSeriesLoader.load(
                config.csvPath(),
                config.symbol() + "-" + config.timeframe(),
                Timeframes.parse(config.timeframe()));
    }

    private CsvMarketSnapshotProvider createMarketSnapshotProvider(BacktestConfig config, BarSeries series) {
        return new CsvMarketSnapshotProvider(series, config.symbol(), config.timeframe());
    }

    private SimulatedPortfolioService createPortfolioService(BacktestConfig config) {
        var portfolioService = new SimulatedPortfolioService();
        portfolioService.seedCash(config.symbol(), config.portfolio().initialCash());
        return portfolioService;
    }

    private StrategyActionEvaluator createStrategyEvaluator(BacktestConfig config, BarSeries series) {
        return strategyFactory.create(
                config.strategy(),
                StrategyEvaluatorContext.ta4j(series),
                config.adxFilter());
    }

    private List<RuntimeCycleResult> runHistoricalCycles(
            BacktestConfig config,
            CsvMarketSnapshotProvider marketSnapshotProvider,
            SimulatedPortfolioService portfolioService,
            StrategyActionEvaluator evaluator,
            SimulatedExecutionService executionService) {
        int cycleCount = marketSnapshotProvider.snapshotCount();
        if (cycleCount <= 0) {
            throw new IllegalArgumentException("cycleCount must be greater than zero.");
        }

        var results = new ArrayList<RuntimeCycleResult>(cycleCount);
        TradeAction pendingAction = TradeAction.HOLD;
        for (int cycle = 0; cycle < cycleCount; cycle++) {
            var snapshot = marketSnapshotProvider.load(config.toTradingDefinition());
            var positionBeforeExecution = portfolioService.getSnapshot(config.symbol()).position();
            Result executionResult = pendingAction == TradeAction.HOLD
                    ? new Result(Status.SKIPPED, false, positionBeforeExecution.open(), "No pending signal to execute.")
                    : executionService.execute(new Request(
                            config.toTradingDefinition().runtimeId(),
                            config.symbol(),
                            config.timeframe(),
                            pendingAction,
                            snapshot.observedAt().minus(Timeframes.parse(config.timeframe())),
                            snapshot.executionPrice()));

            var portfolioAfterExecution = portfolioService.getSnapshot(config.symbol());
            results.add(new RuntimeCycleResult(
                    snapshot,
                    portfolioAfterExecution,
                    pendingAction,
                    executionResult));

            pendingAction = evaluator.evaluate(new ActionContext(
                    portfolioAfterExecution.position().open(),
                    snapshot.barIndex()));
        }
        return List.copyOf(results);
    }
}
