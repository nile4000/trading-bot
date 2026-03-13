package ch.lueem.tradingbot.adapters.execution.binance.client;

import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.common.configuration.SignatureConfiguration;

/**
 * Creates Binance REST clients. The default base URL points to Spot Testnet.
 */
public class BinanceClientFactory {

    public static final String TESTNET_REST_BASE_URL = "https://testnet.binance.vision";

    private final String baseUrl;

    public BinanceClientFactory() {
        this(TESTNET_REST_BASE_URL);
    }

    public BinanceClientFactory(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be null or blank.");
        }
        this.baseUrl = baseUrl;
    }

    public BinanceClient create(String apiKey, String secretKey) {
        return new DefaultBinanceClient(createClientConfiguration(apiKey, secretKey));
    }

    ClientConfiguration createClientConfiguration(String apiKey, String secretKey) {
        validateKeys(apiKey, secretKey);

        var signatureConfig = new SignatureConfiguration();
        signatureConfig.setApiKey(apiKey);
        signatureConfig.setSecretKey(secretKey);

        var clientConfig = new ClientConfiguration();
        clientConfig.setUrl(baseUrl);
        clientConfig.setSignatureConfiguration(signatureConfig);

        return clientConfig;
    }

    private void validateKeys(String apiKey, String secretKey) {
        if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("apiKey and secretKey must not be null or blank.");
        }
    }
}
