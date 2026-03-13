package ch.lueem.tradingbot.quarkus;

import java.util.concurrent.Callable;

import ch.lueem.tradingbot.adapters.config.ReportingConfig;
import ch.lueem.tradingbot.adapters.reporting.BacktestReportJsonPrinter;
import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.modes.backtest.Runner;
import ch.lueem.tradingbot.modes.backtest.model.Report;
import jakarta.inject.Inject;
import picocli.CommandLine;
import org.jboss.logging.Logger;

/**
 * Runs one configured backtest inside the Quarkus command runtime.
 */
@CommandLine.Command(name = "backtest")
public class BacktestCommand implements Callable<Integer> {

    private static final Logger LOG = Logger.getLogger(BacktestCommand.class);

    @Inject
    Runner runner;
    @Inject
    BacktestReportJsonPrinter reportPrinter;
    @Inject
    BacktestConfig backtestConfig;
    @Inject
    ReportingConfig reportingConfig;
    @Inject
    TradingBotRuntimeConfig runtimeConfig;

    @Override
    public Integer call() {
        logLifecycleStart();

        var report = runner.backtest(backtestConfig);

        logLifecycleFinish(report);
        reportPrinter.print(System.out, reportingConfig, backtestConfig, report);
        return CommandLine.ExitCode.OK;
    }

    private void logLifecycleStart() {
        if (!runtimeConfig.app().lifecycleEvents()) {
            return;
        }
        LOG.infof(
                "Starting backtest. symbol=%s, timeframe=%s, strategy=%s, csvPath=%s",
                backtestConfig.symbol(),
                backtestConfig.timeframe(),
                backtestConfig.strategy().name(),
                backtestConfig.csvPath());
    }

    private void logLifecycleFinish(Report report) {
        if (!runtimeConfig.app().lifecycleEvents()) {
            return;
        }
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
