package ch.lueem.tradingbot.integration.binance.spottestnet;

import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.common.configuration.SignatureConfiguration;

/**
 * Creates Spot Testnet REST clients with explicit testnet base URLs.
 */
public class BinanceSpotTestnetClientFactory {

    public static final String TESTNET_REST_BASE_URL = "https://testnet.binance.vision";

    public BinanceSpotTestnetClient create(String apiKey, String secretKey) {
        return new DefaultBinanceSpotTestnetClient(createClientConfiguration(apiKey, secretKey));
    }

    public ClientConfiguration createClientConfiguration(String apiKey, String secretKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be blank.");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("secretKey must not be blank.");
        }

        SignatureConfiguration signatureConfiguration = new SignatureConfiguration();
        signatureConfiguration.setApiKey(apiKey);
        signatureConfiguration.setSecretKey(secretKey);

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setUrl(TESTNET_REST_BASE_URL);
        clientConfiguration.setSignatureConfiguration(signatureConfiguration);
        return clientConfiguration;
    }
}
