package ch.lueem.tradingbot.application;

import java.io.PrintStream;

/**
 * Dispatches the loaded application configuration to the matching entry flow.
 */
public class ApplicationLauncher {

    private final BacktestService backtestService;
    private final PaperBotService paperBotService;

    public ApplicationLauncher() {
        this(new BacktestService(), new PaperBotService());
    }

    public ApplicationLauncher(BacktestService backtestService, PaperBotService paperBotService) {
        if (backtestService == null) {
            throw new IllegalArgumentException("backtestService must not be null.");
        }
        if (paperBotService == null) {
            throw new IllegalArgumentException("paperBotService must not be null.");
        }
        this.backtestService = backtestService;
        this.paperBotService = paperBotService;
    }

    public void run(ApplicationConfig config, PrintStream out) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("out must not be null.");
        }

        switch (config.mode()) {
            case BACKTEST -> backtestService.run(config, out);
            case PAPER -> paperBotService.run(config);
        }
    }
}
