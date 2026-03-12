package ch.lueem.tradingbot.modes.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.BinanceSpotTestnetConfig;
import ch.lueem.tradingbot.adapters.config.PaperActionSourceConfig;
import ch.lueem.tradingbot.adapters.config.PaperBotConfig;
import ch.lueem.tradingbot.adapters.config.PaperConfig;
import ch.lueem.tradingbot.adapters.config.PaperExchange;
import ch.lueem.tradingbot.adapters.config.PaperExecutionConfig;
import ch.lueem.tradingbot.adapters.config.PaperOrderMode;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
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

    private PaperConfig config() {
        return new PaperConfig(
                new PaperBotConfig("bot-1", "v1", "BTCUSDT", "1m"),
                new PaperExecutionConfig(
                        PaperExchange.BINANCE_SPOT_TESTNET,
                        PaperOrderMode.VALIDATE_ONLY,
                        1000L,
                        1000.0,
                        new BigDecimal("0.0010")),
                new PaperActionSourceConfig("queued_actions", List.of(TradeAction.BUY)),
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
    }
}
