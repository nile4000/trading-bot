package ch.lueem.tradingbot.quarkus;

import java.util.concurrent.Callable;

import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.modes.paper.PaperBotLoop;
import ch.lueem.tradingbot.modes.paper.PaperBotSetup;
import jakarta.inject.Inject;
import picocli.CommandLine;

/**
 * Runs the configured paper bot loop inside the Quarkus command runtime.
 */
@CommandLine.Command(name = "paper")
public class PaperCommand implements Callable<Integer> {

    @Inject
    PaperBotSetup setup;
    @Inject
    PaperBotLoop loop;
    @Inject
    PaperConfig paperConfig;
    @Inject
    TradingBotRuntimeConfig runtimeConfig;

    @Override
    public Integer call() {
        loop.run(
                setup.createSession(paperConfig),
                runtimeConfig.app().lifecycleEvents());
        return CommandLine.ExitCode.OK;
    }
}
