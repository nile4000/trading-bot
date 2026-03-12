package ch.lueem.tradingbot.integration.binance.spottestnet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.binance.connector.client.common.configuration.ClientConfiguration;
import org.junit.jupiter.api.Test;

class BinanceSpotTestnetClientFactoryTest {

    @Test
    void createClientConfiguration_overridesDefaultBaseUrlToTestnet() {
        BinanceSpotTestnetClientFactory factory = new BinanceSpotTestnetClientFactory();

        ClientConfiguration configuration = factory.createClientConfiguration("api-key", "secret-key");

        assertEquals(BinanceSpotTestnetClientFactory.TESTNET_REST_BASE_URL, configuration.getUrl());
    }
}
