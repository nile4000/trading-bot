package ch.lueem.tradingbot.adapters.config;

/**
 * Holds Binance Spot Testnet connection settings resolved by the paper bot.
 */
public record BinanceSpotTestnetConfig(
        String apiKeyEnv,
        String secretKeyEnv,
        double recvWindowMillis
) {
    public void validate() {
        if (apiKeyEnv == null || apiKeyEnv.isBlank()) {
            throw new IllegalStateException("paper.binance.apiKeyEnv must not be blank.");
        }
        if (secretKeyEnv == null || secretKeyEnv.isBlank()) {
            throw new IllegalStateException("paper.binance.secretKeyEnv must not be blank.");
        }
        if (recvWindowMillis <= 0.0) {
            throw new IllegalStateException("paper.binance.recvWindowMillis must be greater than zero.");
        }
    }
}
