package ch.lueem.tradingbot;

import ch.lueem.tradingbot.application.ApplicationLauncher;
import ch.lueem.tradingbot.application.ApplicationConfig;
import ch.lueem.tradingbot.application.ApplicationConfigLoader;

/**
 * Starts the application from the YAML-based local configuration.
 */
public class App {

    public static void main(String[] args) {
        ApplicationConfig config = new ApplicationConfigLoader().load();
        new ApplicationLauncher().run(config, System.out);
    }
}
