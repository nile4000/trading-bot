package ch.lueem.tradingbot.quarkus;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

/**
 * Top-level command for the Quarkus-hosted trading bot runtime.
 */
@TopCommand
@CommandLine.Command(
        name = "trading-bot",
        mixinStandardHelpOptions = true,
        description = "Runs the trading bot in backtest or paper mode.",
        subcommands = {
                BacktestCommand.class,
                PaperCommand.class
        })
public class TradingBotCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec commandSpec;

    @Override
    public void run() {
        commandSpec.commandLine().usage(System.out);
    }
}
