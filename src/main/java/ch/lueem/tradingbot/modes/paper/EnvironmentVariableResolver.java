package ch.lueem.tradingbot.modes.paper;

/**
 * Resolves environment variables used for secret lookup.
 */
@FunctionalInterface
public interface EnvironmentVariableResolver {

    String get(String name);
}
