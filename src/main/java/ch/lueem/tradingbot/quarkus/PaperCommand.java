package ch.lueem.tradingbot.quarkus;

import java.util.concurrent.Callable;

import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.modes.paper.PaperBotService;
import jakarta.inject.Inject;
import picocli.CommandLine;

/**
 * Runs the configured paper bot loop inside the Quarkus command runtime.
 */
@CommandLine.Command(name = "paper")
public class PaperCommand implements Callable<Integer> {

    @Inject
    PaperBotService paperBotService;
    @Inject
    PaperConfig paperConfig;
    @Inject
    TradingBotRuntimeConfig runtimeConfig;

    @Override
    public Integer call() {
        paperBotService.run(paperConfig, runtimeConfig.app().lifecycleEvents());
        return CommandLine.ExitCode.OK;
    }
}
