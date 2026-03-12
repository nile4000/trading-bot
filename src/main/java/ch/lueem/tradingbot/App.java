package ch.lueem.tradingbot;

import ch.lueem.tradingbot.application.ApplicationConfig;
import ch.lueem.tradingbot.application.ApplicationConfigLoader;
import ch.lueem.tradingbot.application.BacktestService;

/**
 * Starts the application from the YAML-based local configuration.
 */
public class App {

    public static void main(String[] args) {
        try {
            ApplicationConfig config = new ApplicationConfigLoader().load();
            new BacktestService().run(config, System.out);
        } catch (RuntimeException exception) {
            System.err.println("Backtest failed: " + exception.getMessage());
        }
    }
}
