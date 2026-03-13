package ch.lueem.tradingbot.quarkus;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;

/**
 * Starts the Quarkus-hosted command runtime for backtest and paper flows.
 */
@QuarkusMain
public class TradingBotMain implements QuarkusApplication {

    @Inject
    CommandLine.IFactory factory;

    @Inject
    @TopCommand
    TradingBotCommand command;

    public static void main(String... args) {
        Quarkus.run(TradingBotMain.class, args);
    }

    @Override
    public int run(String... args) {
        return new CommandLine(command, factory).execute(args);
    }
}
