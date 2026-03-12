package ch.lueem.tradingbot.strategy;

/**
 * Describes one strategy together with its concrete parameter set.
 */
public record StrategyDefinition(
        String name,
        StrategyParameters parameters
) {
}
