package ch.lueem.tradingbot.adapters.config;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Loads the application configuration from the classpath YAML file.
 */
public class ApplicationConfigLoader {

    private final String configResource;
    private final ObjectMapper objectMapper;

    public ApplicationConfigLoader() {
        this("application.yml");
    }

    public ApplicationConfigLoader(String configResource) {
        if (configResource == null || configResource.isBlank()) {
            throw new IllegalArgumentException("configResource must not be blank.");
        }
        this.configResource = configResource;
        this.objectMapper = new ObjectMapper(new YAMLFactory())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public ApplicationConfig load() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configResource)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing configuration resource: " + configResource);
            }

            ApplicationConfig config = objectMapper.readValue(inputStream, ApplicationConfig.class);
            validateConfig(config);
            return config;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load configuration from " + configResource, exception);
        }
    }

    private void validateConfig(ApplicationConfig config) {
        if (config == null) {
            throw new IllegalStateException("Application configuration must not be null.");
        }
        if (config.mode() == null) {
            throw new IllegalStateException("Missing 'mode' in application.yml.");
        }
        if (config.reporting() == null) {
            throw new IllegalStateException("Missing 'reporting' section in application.yml.");
        }
        if (config.logging() == null) {
            throw new IllegalStateException("Missing 'logging' section in application.yml.");
        }

        if (config.mode() == ApplicationMode.BACKTEST) {
            if (config.backtest() == null) {
                throw new IllegalStateException("Missing 'backtest' section in application.yml.");
            }
            config.backtest().validate();
            return;
        }

        if (config.mode() == ApplicationMode.PAPER) {
            if (config.paper() == null) {
                throw new IllegalStateException("Missing 'paper' section in application.yml.");
            }
            config.paper().validate();
            return;
        }

        throw new IllegalStateException("Unsupported mode in application.yml: " + config.mode());
    }
}
