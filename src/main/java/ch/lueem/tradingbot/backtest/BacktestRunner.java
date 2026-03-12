package ch.lueem.tradingbot.backtest;

import java.nio.file.Path;
import java.time.Duration;

import ch.lueem.tradingbot.application.BacktestRequest;
import ch.lueem.tradingbot.backtest.calc.BacktestMetricsCalculator;
import ch.lueem.tradingbot.backtest.calc.BacktestPositionReportBuilder;
import ch.lueem.tradingbot.backtest.data.CsvBarSeriesLoader;
import ch.lueem.tradingbot.backtest.model.BacktestReport;
import ch.lueem.tradingbot.strategy.ta4j.EmaCrossStrategyFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.backtest.BarSeriesManager;

/**
 * Coordinates CSV loading, strategy creation and result calculation for one backtest run.
 */
public class BacktestRunner {

    private final CsvBarSeriesLoader csvBarSeriesLoader;
    private final EmaCrossStrategyFactory strategyFactory;
    private final BacktestPositionReportBuilder positionReportBuilder;
    private final BacktestMetricsCalculator metricsCalculator;

    public BacktestRunner() {
        this(
                new CsvBarSeriesLoader(),
                new EmaCrossStrategyFactory(),
                new BacktestPositionReportBuilder(),
                new BacktestMetricsCalculator());
    }

    public BacktestRunner(
            CsvBarSeriesLoader csvBarSeriesLoader,
            EmaCrossStrategyFactory strategyFactory,
            BacktestPositionReportBuilder positionReportBuilder,
            BacktestMetricsCalculator metricsCalculator) {
        this.csvBarSeriesLoader = csvBarSeriesLoader;
        this.strategyFactory = strategyFactory;
        this.positionReportBuilder = positionReportBuilder;
        this.metricsCalculator = metricsCalculator;
    }

    public BacktestReport run(BacktestRequest request) {
        validateInputs(request);

        Duration barDuration = parseTimeframe(request.timeframe());
        BarSeries series = csvBarSeriesLoader.load(
                request.csvPath(),
                request.symbol() + "-" + request.timeframe(),
                barDuration);
        Strategy strategy = strategyFactory.create(series, request.shortEma(), request.longEma());
        TradingRecord tradingRecord = new BarSeriesManager(series).run(strategy);
        var positionReports = positionReportBuilder.build(series, tradingRecord, request.initialCash());

        return metricsCalculator.calculate(
                series,
                tradingRecord,
                request.symbol(),
                request.timeframe(),
                request.initialCash(),
                request.strategy(),
                positionReports);
    }

    private void validateInputs(BacktestRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null.");
        }
        if (request.csvPath() == null) {
            throw new IllegalArgumentException("csvPath must not be null.");
        }
        if (request.symbol() == null || request.symbol().isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank.");
        }
        if (request.timeframe() == null || request.timeframe().isBlank()) {
            throw new IllegalArgumentException("timeframe must not be blank.");
        }
        if (request.strategy() == null) {
            throw new IllegalArgumentException("strategy must not be null.");
        }
        if (request.initialCash() <= 0.0) {
            throw new IllegalArgumentException("initialCash must be greater than zero.");
        }
    }

    private Duration parseTimeframe(String timeframe) {
        return switch (timeframe) {
            case "1m" -> Duration.ofMinutes(1);
            case "5m" -> Duration.ofMinutes(5);
            case "15m" -> Duration.ofMinutes(15);
            case "1h" -> Duration.ofHours(1);
            case "4h" -> Duration.ofHours(4);
            case "1d" -> Duration.ofDays(1);
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        };
    }
}
