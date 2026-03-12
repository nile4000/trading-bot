package ch.lueem.tradingbot.strategy.definition;

/**
 * Holds the parameter set for one strategy definition.
 */
public record StrategyParameters(
        int shortEma,
        int longEma
) {
}
