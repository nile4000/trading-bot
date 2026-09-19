package ch.lueem.tradingbot.core.strategy;

/** Configures the optional ADX entry filter for strategy evaluation. */
public record AdxFilterConfig(
        boolean enabled,
        int period,
        int minimumStrength) {
}
