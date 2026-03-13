package ch.lueem.tradingbot.adapters.config.paper;

import static ch.lueem.tradingbot.adapters.config.ConfigValidation.requirePositive;

/**
 * Holds Binance connection settings resolved by the paper bot.
 */
public record BinanceConfig(
        String apiKey,
        String secretKey,
        double recvWindowMillis
) {
    public BinanceConfig {
        requirePositive(recvWindowMillis, "paper.binance.recvWindowMillis must be greater than zero.");
    }
}
