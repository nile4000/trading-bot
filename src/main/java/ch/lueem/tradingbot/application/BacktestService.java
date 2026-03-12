package ch.lueem.tradingbot.application;

import java.io.PrintStream;

import ch.lueem.tradingbot.backtest.BacktestReport;
import ch.lueem.tradingbot.backtest.BacktestRunner;
import ch.lueem.tradingbot.reporting.BacktestReportJsonPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the backtest use case end-to-end and delegates technical logging and
 * report output.
 */
public class BacktestService {

    private static final Logger LOG = LoggerFactory.getLogger(BacktestService.class);

    private final BacktestRunner backtestRunner;
    private final BacktestReportJsonPrinter reportPrinter;

    public BacktestService() {
        this(new BacktestRunner(), new BacktestReportJsonPrinter());
    }

    public BacktestService(BacktestRunner backtestRunner, BacktestReportJsonPrinter reportPrinter) {
        this.backtestRunner = backtestRunner;
        this.reportPrinter = reportPrinter;
    }

    public void run(ApplicationConfig config, PrintStream out) {
        validateInputs(config, out);
        BacktestRequest request = config.backtest().toRequest();

        logLifecycleStart(config.logging(), request);

        BacktestReport report = backtestRunner.run(
                request.csvPath(),
                request.symbol(),
                request.timeframe(),
                request.shortEma(),
                request.longEma(),
                request.initialCash());

        logLifecycleFinish(config.logging(), report);

        reportPrinter.print(
                out,
                config.reporting(),
                request,
                report);
    }

    private void validateInputs(ApplicationConfig config, PrintStream out) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("out must not be null.");
        }
    }

    private void logLifecycleStart(LoggingConfig logging, BacktestRequest request) {
        if (logging.lifecycleEvents()) {
            LOG.info(
                    "Starting backtest. symbol={}, timeframe={}, strategy={}, csvPath={}",
                    request.symbol(),
                    request.timeframe(),
                    request.strategyName(),
                    request.csvPath());
        }
    }

    private void logLifecycleFinish(LoggingConfig logging, BacktestReport report) {
        if (logging.lifecycleEvents()) {
            LOG.info(
                    "Backtest finished. bars={}, closedTrades={}, returnPct={}",
                    report.barCount(),
                    report.tradeCount(),
                    "%.2f".formatted(report.returnPct()));
        }
    }
}
