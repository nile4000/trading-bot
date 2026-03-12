package ch.lueem.tradingbot.core.strategy.definition;

/**
 * Describes one strategy together with its concrete parameter set.
 */
public record StrategyDefinition(
        String name,
        StrategyParameters parameters
) {
}
