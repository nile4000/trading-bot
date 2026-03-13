package ch.lueem.tradingbot.adapters.config.paper;

/**
 * Holds Binance connection settings resolved by the paper bot.
 */
public record BinanceConfig(
        String apiKey,
        String secretKey,
        double recvWindowMillis
) {
}
