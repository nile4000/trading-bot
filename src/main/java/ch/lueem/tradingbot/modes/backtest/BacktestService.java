package ch.lueem.tradingbot.modes.backtest;

import java.io.PrintStream;

import ch.lueem.tradingbot.adapters.config.ReportingConfig;
import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.adapters.reporting.BacktestReportJsonPrinter;
import ch.lueem.tradingbot.modes.backtest.model.Report;
import org.jboss.logging.Logger;

/**
 * Runs the backtest use case end-to-end and delegates technical logging and
 * report output.
 */
public class BacktestService {

    private static final Logger LOG = Logger.getLogger(BacktestService.class);

    private final Runner runner;
    private final BacktestReportJsonPrinter reportPrinter;

    public BacktestService() {
        this(new Runner(), new BacktestReportJsonPrinter());
    }

    public BacktestService(Runner runner, BacktestReportJsonPrinter reportPrinter) {
        this.runner = runner;
        this.reportPrinter = reportPrinter;
    }

    public void run(
            BacktestConfig backtest,
            ReportingConfig reporting,
            boolean lifecycleEvents,
            PrintStream out) {
        logLifecycleStart(lifecycleEvents, backtest);

        var report = runner.backtest(backtest);

        logLifecycleFinish(lifecycleEvents, report);

        reportPrinter.print(
                out,
                reporting,
                backtest,
                report);
    }

    private void logLifecycleStart(boolean lifecycleEvents, BacktestConfig backtest) {
        if (lifecycleEvents) {
            LOG.infof(
                    "Starting backtest. symbol=%s, timeframe=%s, strategy=%s, csvPath=%s",
                    backtest.symbol(),
                    backtest.timeframe(),
                    backtest.strategy().name(),
                    backtest.csvPath());
        }
    }

    private void logLifecycleFinish(boolean lifecycleEvents, Report report) {
        if (lifecycleEvents) {
            LOG.infof(
                    "Backtest finished. mode=%s, symbol=%s, timeframe=%s, barCount=%d, dataStart=%s, dataEnd=%s, executionModel=%s, closedTradeCount=%d, totalReturnPercent=%s",
                    report.metadata().mode(),
                    report.metadata().symbol(),
                    report.metadata().timeframe(),
                    report.metadata().barCount(),
                    report.metadata().dataStart(),
                    report.metadata().dataEnd(),
                    report.metadata().executionModel(),
                    report.closedTradeCount(),
                    report.totalReturnPercent());
        }
    }
}
