package ch.lueem.tradingbot.backtest;

import java.math.BigDecimal;

import ch.lueem.tradingbot.application.BacktestConfig;
import ch.lueem.tradingbot.backtest.data.CsvBarSeriesLoader;
import ch.lueem.tradingbot.backtest.model.BacktestReport;
import ch.lueem.tradingbot.integration.backtest.CsvHistoricalMarketSnapshotProvider;
import ch.lueem.tradingbot.integration.simulation.SimulatedExecutionService;
import ch.lueem.tradingbot.integration.simulation.SimulatedPortfolioService;
import ch.lueem.tradingbot.runtime.TradingRuntime;
import ch.lueem.tradingbot.strategy.action.EmaCrossActionEvaluator;

/**
 * Coordinates CSV loading, strategy creation and result calculation for one backtest run.
 */
public class BacktestRunner {

    private final CsvBarSeriesLoader csvBarSeriesLoader;
    private final HistoricalRuntimeRunner historicalRuntimeRunner;
    private final BacktestReportBuilder reportBuilder;

    public BacktestRunner() {
        this(
                new CsvBarSeriesLoader(),
                new HistoricalRuntimeRunner(),
                new BacktestReportBuilder());
    }

    public BacktestRunner(
            CsvBarSeriesLoader csvBarSeriesLoader,
            HistoricalRuntimeRunner historicalRuntimeRunner,
            BacktestReportBuilder reportBuilder) {
        this.csvBarSeriesLoader = csvBarSeriesLoader;
        this.historicalRuntimeRunner = historicalRuntimeRunner;
        this.reportBuilder = reportBuilder;
    }

    public BacktestReport run(BacktestConfig config) {
        validateInputs(config);

        CsvHistoricalMarketSnapshotProvider marketSnapshotProvider = new CsvHistoricalMarketSnapshotProvider(
                csvBarSeriesLoader,
                config.csvPath(),
                config.symbol(),
                config.timeframe());
        SimulatedPortfolioService portfolioService = new SimulatedPortfolioService();
        portfolioService.seedCash(config.symbol(), BigDecimal.valueOf(config.portfolio().initialCash()));

        TradingRuntime runtime = new TradingRuntime(
                config.toTradingDefinition(),
                marketSnapshotProvider,
                portfolioService,
                new EmaCrossActionEvaluator(config.strategy().parameters()),
                new SimulatedExecutionService(portfolioService));

        return reportBuilder.assemble(
                config,
                historicalRuntimeRunner.run(runtime, marketSnapshotProvider.snapshotCount()));
    }

    private void validateInputs(BacktestConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null.");
        }
        config.validate();
    }
}
