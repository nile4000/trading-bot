package ch.lueem.tradingbot.adapters.config.backtest;

/** Configures the optional ADX entry filter for historical simulations. */
public record AdxFilterConfig(
        boolean enabled,
        int period,
        int minimumStrength) {
}
