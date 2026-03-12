package ch.lueem.tradingbot.application;

/**
 * Holds the configured strategy parameters for the backtest.
 */
public record StrategyConfig(
        String name,
        int shortEma,
        int longEma
) {
}
