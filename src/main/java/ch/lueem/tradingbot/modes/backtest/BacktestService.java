package ch.lueem.tradingbot.modes.backtest;

import java.io.PrintStream;

import ch.lueem.tradingbot.adapters.config.ApplicationConfig;
import ch.lueem.tradingbot.adapters.config.BacktestConfig;
import ch.lueem.tradingbot.adapters.config.LoggingConfig;
import ch.lueem.tradingbot.modes.backtest.model.BacktestReport;
import ch.lueem.tradingbot.adapters.reporting.BacktestReportJsonPrinter;
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
        BacktestConfig backtest = config.backtest();

        logLifecycleStart(config.logging(), backtest);

        BacktestReport report = backtestRunner.run(backtest);

        logLifecycleFinish(config.logging(), report);

        reportPrinter.print(
                out,
                config.reporting(),
                backtest,
                report);
    }

    private void logLifecycleStart(LoggingConfig logging, BacktestConfig backtest) {
        if (logging.lifecycleEvents()) {
            LOG.info(
                    "Starting backtest. symbol={}, timeframe={}, strategy={}, csvPath={}",
                    backtest.symbol(),
                    backtest.timeframe(),
                    backtest.strategy().name(),
                    backtest.csvPath());
        }
    }

    private void logLifecycleFinish(LoggingConfig logging, BacktestReport report) {
        if (logging.lifecycleEvents()) {
            LOG.info(
                    "Backtest finished. mode={}, symbol={}, timeframe={}, barCount={}, dataStart={}, dataEnd={}, executionModel={}, closedTradeCount={}, totalReturnPercent={}",
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
