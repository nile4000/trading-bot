package ch.lueem.tradingbot.app;

import ch.lueem.tradingbot.app.ApplicationLauncher;
import ch.lueem.tradingbot.adapters.config.ApplicationConfig;
import ch.lueem.tradingbot.adapters.config.ApplicationConfigLoader;

/**
 * Starts the application from the YAML-based local configuration.
 */
public class App {

    public static void main(String[] args) {
        ApplicationConfig config = new ApplicationConfigLoader().load();
        new ApplicationLauncher().run(config, System.out);
    }
}
