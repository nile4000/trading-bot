package ch.lueem.tradingbot.adapters.execution.binance.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BinanceClientFactoryTest {

    @Test
    void createsClientConfigurationWithTestnetBaseUrlByDefault() {
        BinanceClientFactory factory = new BinanceClientFactory();

        var configuration = factory.createClientConfiguration("key", "secret");

        assertEquals(BinanceClientFactory.TESTNET_REST_BASE_URL, configuration.getUrl());
    }
}
