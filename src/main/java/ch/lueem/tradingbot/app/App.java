package ch.lueem.tradingbot.app;

import ch.lueem.tradingbot.adapters.config.ApplicationConfig;
import ch.lueem.tradingbot.adapters.config.ApplicationConfigLoader;

/**
 * Starts the application from the YAML-based local configuration.
 */
public class App {

    static final String DEFAULT_CONFIG = "application.yml";
    static final String CONFIG_PROPERTY = "trading.config";
    static final String CONFIG_ENV = "TRADING_CONFIG";

    public static void main(String[] args) {
        ApplicationConfig config = new ApplicationConfigLoader(
                resolveConfigResource(
                        args,
                        System.getProperty(CONFIG_PROPERTY),
                        System.getenv(CONFIG_ENV)))
                .load();
        new ApplicationLauncher().run(config, System.out);
    }

    static String resolveConfigResource(String[] args, String systemPropertyValue, String environmentValue) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return args[0];
        }
        if (systemPropertyValue != null && !systemPropertyValue.isBlank()) {
            return systemPropertyValue;
        }
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        return DEFAULT_CONFIG;
    }
}
