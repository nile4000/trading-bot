package ch.lueem.tradingbot.application;

/**
 * Resolves environment variables used for secret lookup.
 */
@FunctionalInterface
public interface EnvironmentVariableResolver {

    String get(String name);
}
