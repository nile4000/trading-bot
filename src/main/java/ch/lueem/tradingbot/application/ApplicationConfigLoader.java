package ch.lueem.tradingbot.application;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Loads the application configuration from the classpath YAML file.
 */
public class ApplicationConfigLoader {

    private static final String CONFIG_RESOURCE = "application.yml";

    private final ObjectMapper objectMapper;

    public ApplicationConfigLoader() {
        this.objectMapper = new ObjectMapper(new YAMLFactory())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public ApplicationConfig load() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing configuration resource: " + CONFIG_RESOURCE);
            }

            ApplicationConfig config = objectMapper.readValue(inputStream, ApplicationConfig.class);
            validateConfig(config);
            return config;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load configuration from " + CONFIG_RESOURCE, exception);
        }
    }

    private void validateConfig(ApplicationConfig config) {
        if (config == null) {
            throw new IllegalStateException("Application configuration must not be null.");
        }
        if (config.backtest() == null) {
            throw new IllegalStateException("Missing 'backtest' section in application.yml.");
        }
        if (config.reporting() == null) {
            throw new IllegalStateException("Missing 'reporting' section in application.yml.");
        }
        if (config.logging() == null) {
            throw new IllegalStateException("Missing 'logging' section in application.yml.");
        }
        config.backtest().toRequest();
    }
}
