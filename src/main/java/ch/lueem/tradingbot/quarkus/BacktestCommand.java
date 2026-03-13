package ch.lueem.tradingbot.quarkus;

import java.util.concurrent.Callable;

import ch.lueem.tradingbot.adapters.config.ReportingConfig;
import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.modes.backtest.BacktestService;
import jakarta.inject.Inject;
import picocli.CommandLine;

/**
 * Runs one configured backtest inside the Quarkus command runtime.
 */
@CommandLine.Command(name = "backtest")
public class BacktestCommand implements Callable<Integer> {

    @Inject
    BacktestService backtestService;
    @Inject
    BacktestConfig backtestConfig;
    @Inject
    ReportingConfig reportingConfig;
    @Inject
    TradingBotRuntimeConfig runtimeConfig;

    @Override
    public Integer call() {
        backtestService.run(
                backtestConfig,
                reportingConfig,
                runtimeConfig.app().lifecycleEvents(),
                System.out);
        return CommandLine.ExitCode.OK;
    }
}
