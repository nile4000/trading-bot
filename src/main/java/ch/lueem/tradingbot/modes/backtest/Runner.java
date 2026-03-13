package ch.lueem.tradingbot.modes.backtest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.adapters.market.CsvBarSeriesLoader;
import ch.lueem.tradingbot.modes.backtest.model.Report;
import ch.lueem.tradingbot.adapters.market.CsvHistoricalMarketSnapshotProvider;
import ch.lueem.tradingbot.adapters.execution.simulated.SimulatedExecutionService;
import ch.lueem.tradingbot.adapters.portfolio.SimulatedPortfolioService;
import ch.lueem.tradingbot.core.runtime.RuntimeCycleResult;
import ch.lueem.tradingbot.core.runtime.TradingRuntime;
import ch.lueem.tradingbot.core.strategy.StrategyEvaluatorContext;
import ch.lueem.tradingbot.core.strategy.StrategyEvaluatorFactory;
import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import org.ta4j.core.BarSeries;

/**
 * Coordinates CSV loading, strategy creation and result calculation for one
 * backtest run.
 */
public class Runner {

    private final CsvBarSeriesLoader csvBarSeriesLoader;
    private final ReportGenerator reportGenerator;
    private final StrategyEvaluatorFactory strategyFactory;

    public Runner() {
        this(new CsvBarSeriesLoader(),
                new ReportGenerator(),
                new StrategyEvaluatorFactory());
    }

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
        var runtime = new TradingRuntime(
                config.toTradingDefinition(),
                marketSnapshotProvider,
                portfolioService,
                evaluator,
                new SimulatedExecutionService(portfolioService));

        return reportGenerator.assemble(
                config,
                runHistoricalCycles(runtime, marketSnapshotProvider.snapshotCount()));
    }

    private BarSeries createBarSeries(BacktestConfig config) {
        return csvBarSeriesLoader.load(
                config.csvPath(),
                config.symbol() + "-" + config.timeframe(),
                CsvHistoricalMarketSnapshotProvider.parseTimeframe(config.timeframe()));
    }

    private CsvHistoricalMarketSnapshotProvider createMarketSnapshotProvider(BacktestConfig config, BarSeries series) {
        return new CsvHistoricalMarketSnapshotProvider(series, config.symbol(), config.timeframe());
    }

    private SimulatedPortfolioService createPortfolioService(BacktestConfig config) {
        var portfolioService = new SimulatedPortfolioService();
        portfolioService.seedCash(config.symbol(), BigDecimal.valueOf(config.portfolio().initialCash()));
        return portfolioService;
    }

    private StrategyActionEvaluator createStrategyEvaluator(BacktestConfig config, BarSeries series) {
        return strategyFactory.create(config.strategy(), StrategyEvaluatorContext.ta4j(series));
    }

    private List<RuntimeCycleResult> runHistoricalCycles(
            TradingRuntime runtime,
            int cycleCount) {
        if (cycleCount <= 0) {
            throw new IllegalArgumentException("cycleCount must be greater than zero.");
        }

        var results = new ArrayList<RuntimeCycleResult>(cycleCount);
        for (int cycle = 0; cycle < cycleCount; cycle++) {
            results.add(runtime.cycle());
        }
        return List.copyOf(results);
    }
}
