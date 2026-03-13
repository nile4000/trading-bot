package ch.lueem.tradingbot.modes.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.paper.BinanceConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperBotConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperExchange;
import ch.lueem.tradingbot.adapters.config.paper.PaperExecutionConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperOrderMode;
import ch.lueem.tradingbot.adapters.config.paper.PaperStrategyConfig;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;

class PaperBotSetupTest {

    private static final String TEST_REST_BASE_URL = "https://testnet.binance.vision";

    @Test
    void createContext_failsWhenRequiredCredentialsAreMissing() {
        PaperBotSetup setup = new PaperBotSetup(
                new ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClientFactory(TEST_REST_BASE_URL));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> setup.createSession(missingSecretsConfig()));

        assertTrue(exception.getMessage().contains("apiKey and secretKey"));
    }

    @Test
    void createContext_usesResolvedSecretsAndExposesRestBaseUrl() {
        CapturingClientFactory clientFactory = new CapturingClientFactory();
        PaperBotSetup setup = new PaperBotSetup(clientFactory);

        PaperBotSession session = setup.createSession(config());

        assertEquals("api-key", clientFactory.apiKey);
        assertEquals("secret-key", clientFactory.secretKey);
        assertEquals(TEST_REST_BASE_URL, session.restBaseUrl());
    }

    @Test
    void createContext_supportsTa4jPaperStrategy() {
        PaperBotSetup setup = new PaperBotSetup(new CapturingClientFactory());

        PaperBotSession session = setup.createSession(ta4jConfig());

        assertEquals("ema_cross", session.runtime().definition().strategy().name());
        assertNotNull(session.runtime().definition().strategy().parameters());
    }

    @Test
    void createContext_keepsUpdatingTheCurrentPaperBarUntilTheTimeframeChanges() {
        SequencedClientFactory clientFactory = new SequencedClientFactory("10", "9", "12", "13", "14", "15");
        PaperBotSetup setup = new PaperBotSetup(clientFactory);
        PaperBotSession session = setup.createSession(paperConfig(
                new PaperStrategyConfig("ema_cross", new StrategyParameters(1, 2), List.of()),
                new BinanceConfig("api-key", "secret-key", 15000.0)));

        ch.lueem.tradingbot.core.runtime.RuntimeCycleResult result = null;
        for (int cycle = 0; cycle < 6; cycle++) {
            result = session.runtime().cycle();
        }

        assertNotNull(result);
        assertEquals(0, result.marketSnapshot().barIndex());
        assertEquals(1, result.marketSnapshot().closePriceHistory().size());
        assertEquals(new BigDecimal("15"), result.marketSnapshot().closePriceHistory().getFirst());
        assertNotNull(result.action());
    }

    private PaperConfig config() {
        return paperConfig(
                new PaperStrategyConfig("queued_actions", null, List.of(TradeAction.BUY)),
                new BinanceConfig("api-key", "secret-key", 15000.0));
    }

    private PaperConfig missingSecretsConfig() {
        return paperConfig(
                new PaperStrategyConfig("queued_actions", null, List.of(TradeAction.BUY)),
                new BinanceConfig("", "", 15000.0));
    }

    private PaperConfig ta4jConfig() {
        return paperConfig(
                new PaperStrategyConfig("ema_cross", new StrategyParameters(3, 7), List.of()),
                new BinanceConfig("api-key", "secret-key", 15000.0));
    }

    private PaperConfig paperConfig(PaperStrategyConfig strategy, BinanceConfig binance) {
        return new PaperConfig(
                new PaperBotConfig("bot-1", "v1", "BTCUSDT", "1m"),
                new PaperExecutionConfig(
                        PaperExchange.BINANCE_SPOT_TESTNET,
                        PaperOrderMode.VALIDATE_ONLY,
                        1000L,
                        1000.0,
                        new BigDecimal("0.0010"),
                        false,
                        new BigDecimal("25.0")),
                strategy,
                binance);
    }

    private static final class CapturingClientFactory extends ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClientFactory {
        private String apiKey;
        private String secretKey;

        private CapturingClientFactory() {
            super(TEST_REST_BASE_URL);
        }

        @Override
        public ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClient create(
                String apiKey,
                String secretKey) {
            this.apiKey = apiKey;
            this.secretKey = secretKey;
            return new StubClient();
        }
    }

    private static final class SequencedClientFactory extends ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClientFactory {
        private final String[] prices;

        private SequencedClientFactory(String... prices) {
            super(TEST_REST_BASE_URL);
            this.prices = prices;
        }

        @Override
        public ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClient create(
                String apiKey,
                String secretKey) {
            return new SequencedClient(prices);
        }
    }

    private static final class StubClient implements ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClient {
        @Override
        public String baseUrl() {
            return TEST_REST_BASE_URL;
        }

        @Override
        public BigDecimal loadSymbolPrice(String symbol) {
            return new BigDecimal("80000.00");
        }

        @Override
        public void validateOrder(com.binance.connector.client.spot.rest.model.OrderTestRequest request) {
        }

        @Override
        public com.binance.connector.client.spot.rest.model.NewOrderResponse placeOrder(
                com.binance.connector.client.spot.rest.model.NewOrderRequest request) {
            return new com.binance.connector.client.spot.rest.model.NewOrderResponse().orderId(1L);
        }
    }

    private static final class SequencedClient implements ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClient {
        private final java.util.ArrayDeque<BigDecimal> prices = new java.util.ArrayDeque<>();

        private SequencedClient(String... prices) {
            for (String price : prices) {
                this.prices.add(new BigDecimal(price));
            }
        }

        @Override
        public String baseUrl() {
            return TEST_REST_BASE_URL;
        }

        @Override
        public BigDecimal loadSymbolPrice(String symbol) {
            return prices.removeFirst();
        }

        @Override
        public void validateOrder(com.binance.connector.client.spot.rest.model.OrderTestRequest request) {
        }

        @Override
        public com.binance.connector.client.spot.rest.model.NewOrderResponse placeOrder(
                com.binance.connector.client.spot.rest.model.NewOrderRequest request) {
            return new com.binance.connector.client.spot.rest.model.NewOrderResponse().orderId(1L);
        }
    }
}
