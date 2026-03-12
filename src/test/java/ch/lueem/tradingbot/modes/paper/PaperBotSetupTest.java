package ch.lueem.tradingbot.modes.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.BinanceSpotTestnetConfig;
import ch.lueem.tradingbot.adapters.config.PaperBotConfig;
import ch.lueem.tradingbot.adapters.config.PaperConfig;
import ch.lueem.tradingbot.adapters.config.PaperExchange;
import ch.lueem.tradingbot.adapters.config.PaperExecutionConfig;
import ch.lueem.tradingbot.adapters.config.PaperOrderMode;
import ch.lueem.tradingbot.adapters.config.PaperStrategyConfig;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;

class PaperBotSetupTest {

    @Test
    void createContext_failsWhenRequiredCredentialsAreMissing() {
        PaperBotSetup setup = new PaperBotSetup(new StubClientFactory(), name -> null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> setup.createSession(config()));

        assertTrue(exception.getMessage().contains("BINANCE_TESTNET_API_KEY"));
    }

    @Test
    void createContext_usesResolvedSecretsAndExposesRestBaseUrl() {
        CapturingClientFactory clientFactory = new CapturingClientFactory();
        PaperBotSetup setup = new PaperBotSetup(
                clientFactory,
                name -> switch (name) {
                    case "BINANCE_TESTNET_API_KEY" -> "api-key";
                    case "BINANCE_TESTNET_SECRET_KEY" -> "secret-key";
                    default -> null;
                });

        PaperBotSession session = setup.createSession(config());

        assertEquals("api-key", clientFactory.apiKey);
        assertEquals("secret-key", clientFactory.secretKey);
        assertEquals("https://testnet.binance.vision", session.restBaseUrl());
    }

    @Test
    void createContext_supportsTa4jPaperStrategy() {
        PaperBotSetup setup = new PaperBotSetup(
                new CapturingClientFactory(),
                name -> "resolved");

        PaperBotSession session = setup.createSession(ta4jConfig());

        assertEquals("ema_cross", session.runtime().definition().strategy().name());
        assertNotNull(session.runtime().definition().strategy().parameters());
    }

    @Test
    void createContext_runsTa4jStrategyAcrossPaperTicks() {
        SequencedClientFactory clientFactory = new SequencedClientFactory("10", "9", "12", "13", "14", "15");
        PaperBotSetup setup = new PaperBotSetup(clientFactory, name -> "resolved");
        PaperBotSession session = setup.createSession(new PaperConfig(
                new PaperBotConfig("bot-1", "v1", "BTCUSDT", "1m"),
                new PaperExecutionConfig(
                        PaperExchange.BINANCE_SPOT_TESTNET,
                        PaperOrderMode.VALIDATE_ONLY,
                        1000L,
                        1000.0,
                        new BigDecimal("0.0010"),
                        false,
                        new BigDecimal("25.0")),
                new PaperStrategyConfig("ema_cross", new StrategyParameters(1, 2), List.of()),
                new BinanceSpotTestnetConfig("BINANCE_TESTNET_API_KEY", "BINANCE_TESTNET_SECRET_KEY", 15000.0)));

        ch.lueem.tradingbot.core.runtime.RuntimeCycleResult result = null;
        for (int cycle = 0; cycle < 6; cycle++) {
            result = session.runtime().cycle();
        }

        assertNotNull(result);
        assertEquals(5, result.marketSnapshot().barIndex());
        assertEquals(6, result.marketSnapshot().closePriceHistory().size());
        assertNotNull(result.action());
    }

    private PaperConfig config() {
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
                new PaperStrategyConfig("queued_actions", null, List.of(TradeAction.BUY)),
                new BinanceSpotTestnetConfig("BINANCE_TESTNET_API_KEY", "BINANCE_TESTNET_SECRET_KEY", 15000.0));
    }

    private PaperConfig ta4jConfig() {
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
                new PaperStrategyConfig("ema_cross", new StrategyParameters(3, 7), List.of()),
                new BinanceSpotTestnetConfig("BINANCE_TESTNET_API_KEY", "BINANCE_TESTNET_SECRET_KEY", 15000.0));
    }

    private static final class StubClientFactory extends ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClientFactory {
        @Override
        public ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClient create(
                String apiKey,
                String secretKey) {
            throw new AssertionError("factory should not be used when env vars are missing");
        }
    }

    private static final class CapturingClientFactory extends ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClientFactory {
        private String apiKey;
        private String secretKey;

        @Override
        public ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClient create(
                String apiKey,
                String secretKey) {
            this.apiKey = apiKey;
            this.secretKey = secretKey;
            return new StubClient();
        }
    }

    private static final class SequencedClientFactory extends ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClientFactory {
        private final String[] prices;

        private SequencedClientFactory(String... prices) {
            this.prices = prices;
        }

        @Override
        public ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClient create(
                String apiKey,
                String secretKey) {
            return new SequencedClient(prices);
        }
    }

    private static final class StubClient implements ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClient {
        @Override
        public String baseUrl() {
            return "https://testnet.binance.vision";
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

    private static final class SequencedClient implements ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClient {
        private final java.util.ArrayDeque<BigDecimal> prices = new java.util.ArrayDeque<>();

        private SequencedClient(String... prices) {
            for (String price : prices) {
                this.prices.add(new BigDecimal(price));
            }
        }

        @Override
        public String baseUrl() {
            return "https://testnet.binance.vision";
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
