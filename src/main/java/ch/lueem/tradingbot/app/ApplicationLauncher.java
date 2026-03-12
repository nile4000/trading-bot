package ch.lueem.tradingbot.app;

import java.io.PrintStream;

import ch.lueem.tradingbot.adapters.config.ApplicationConfig;
import ch.lueem.tradingbot.modes.backtest.BacktestService;
import ch.lueem.tradingbot.modes.paper.PaperBotService;

/**
 * Selects the matching entry flow for the loaded application configuration.
 */
public class ApplicationLauncher {

    private final BacktestService backtestService;
    private final PaperBotService paperBotService;

    public ApplicationLauncher() {
        this(new BacktestService(), new PaperBotService());
    }

    public ApplicationLauncher(BacktestService backtestService, PaperBotService paperBotService) {
        this.backtestService = backtestService;
        this.paperBotService = paperBotService;
    }

    public void run(ApplicationConfig config, PrintStream out) {
        switch (config.mode()) {
            case BACKTEST -> backtestService.run(config, out);
            case PAPER -> paperBotService.run(config);
        }
    }
}
