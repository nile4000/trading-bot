package ch.lueem.tradingbot.modes.backtest;

import java.math.BigDecimal;

import ch.lueem.tradingbot.adapters.config.BacktestConfig;
import ch.lueem.tradingbot.adapters.market.CsvBarSeriesLoader;
import ch.lueem.tradingbot.modes.backtest.model.BacktestReport;
import ch.lueem.tradingbot.adapters.market.CsvHistoricalMarketSnapshotProvider;
import ch.lueem.tradingbot.adapters.execution.SimulatedExecutionService;
import ch.lueem.tradingbot.adapters.portfolio.SimulatedPortfolioService;
import ch.lueem.tradingbot.core.runtime.TradingRuntime;
import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.ta4j.Ta4jStrategyFactory;
import org.ta4j.core.BarSeries;

/**
 * Coordinates CSV loading, strategy creation and result calculation for one
 * backtest run.
 */
public class BacktestRunner {

    private final CsvBarSeriesLoader csvBarSeriesLoader;
    private final HistoricalRuntimeRunner historicalRuntimeRunner;
    private final BacktestReportBuilder reportBuilder;
    private final Ta4jStrategyFactory strategyFactory;

    public BacktestRunner() {
        this(new CsvBarSeriesLoader(), new HistoricalRuntimeRunner(), new BacktestReportBuilder(), new Ta4jStrategyFactory());
    }

    public BacktestRunner(
            CsvBarSeriesLoader csvBarSeriesLoader,
            HistoricalRuntimeRunner historicalRuntimeRunner,
            BacktestReportBuilder reportBuilder,
            Ta4jStrategyFactory strategyFactory) {
        this.csvBarSeriesLoader = csvBarSeriesLoader;
        this.historicalRuntimeRunner = historicalRuntimeRunner;
        this.reportBuilder = reportBuilder;
        this.strategyFactory = strategyFactory;
    }

    public BacktestReport run(BacktestConfig config) {
        config.validate();

        BarSeries series = createBarSeries(config);
        CsvHistoricalMarketSnapshotProvider marketSnapshotProvider = createMarketSnapshotProvider(config, series);
        SimulatedPortfolioService portfolioService = createPortfolioService(config);
        TradingRuntime runtime = createRuntime(
                config,
                marketSnapshotProvider,
                portfolioService,
                createStrategyEvaluator(config, series));

        return reportBuilder.assemble(
                config,
                historicalRuntimeRunner.run(runtime, marketSnapshotProvider.snapshotCount()));
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
        SimulatedPortfolioService portfolioService = new SimulatedPortfolioService();
        portfolioService.seedCash(config.symbol(), BigDecimal.valueOf(config.portfolio().initialCash()));
        return portfolioService;
    }

    private TradingRuntime createRuntime(
            BacktestConfig config,
            CsvHistoricalMarketSnapshotProvider marketSnapshotProvider,
            SimulatedPortfolioService portfolioService,
            StrategyActionEvaluator evaluator) {
        return new TradingRuntime(
                config.toTradingDefinition(),
                marketSnapshotProvider,
                portfolioService,
                evaluator,
                new SimulatedExecutionService(portfolioService));
    }

    private StrategyActionEvaluator createStrategyEvaluator(BacktestConfig config, BarSeries series) {
        return strategyFactory.create(config.strategy(), series);
    }
}
