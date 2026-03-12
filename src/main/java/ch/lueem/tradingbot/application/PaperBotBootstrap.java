package ch.lueem.tradingbot.application;

import java.math.BigDecimal;

import ch.lueem.tradingbot.integration.binance.spottestnet.BinanceSpotTestnetClient;
import ch.lueem.tradingbot.integration.binance.spottestnet.BinanceSpotTestnetClientFactory;
import ch.lueem.tradingbot.integration.binance.spottestnet.BinanceSpotTestnetExecutionService;
import ch.lueem.tradingbot.integration.binance.spottestnet.BinanceTickerPriceMarketSnapshotProvider;
import ch.lueem.tradingbot.integration.paper.PaperPortfolioService;
import ch.lueem.tradingbot.runtime.TradingRuntime;
import ch.lueem.tradingbot.strategy.action.QueuedActionEvaluator;

/**
 * Resolves secrets and infrastructure dependencies for the configured paper bot runtime.
 */
public class PaperBotBootstrap {

    private final BinanceSpotTestnetClientFactory clientFactory;
    private final EnvironmentVariableResolver environmentVariableResolver;

    public PaperBotBootstrap(
            BinanceSpotTestnetClientFactory clientFactory,
            EnvironmentVariableResolver environmentVariableResolver) {
        if (clientFactory == null) {
            throw new IllegalArgumentException("clientFactory must not be null.");
        }
        if (environmentVariableResolver == null) {
            throw new IllegalArgumentException("environmentVariableResolver must not be null.");
        }
        this.clientFactory = clientFactory;
        this.environmentVariableResolver = environmentVariableResolver;
    }

    public PaperBotRuntimeContext createContext(PaperConfig paper) {
        if (paper == null) {
            throw new IllegalArgumentException("paper must not be null.");
        }
        paper.validate();

        if (paper.execution().exchange() != PaperExchange.BINANCE_SPOT_TESTNET) {
            throw new IllegalStateException("Unsupported paper exchange in phase 1: " + paper.execution().exchange());
        }

        String apiKey = resolveRequiredSecret(paper.binance().apiKeyEnv());
        String secretKey = resolveRequiredSecret(paper.binance().secretKeyEnv());
        BinanceSpotTestnetClient client = clientFactory.create(apiKey, secretKey);
        PaperPortfolioService portfolioService = new PaperPortfolioService(
                paper.bot().symbol(),
                BigDecimal.valueOf(paper.execution().initialCash()));

        TradingRuntime runtime = new TradingRuntime(
                paper.toTradingDefinition(),
                new BinanceTickerPriceMarketSnapshotProvider(client),
                portfolioService,
                new QueuedActionEvaluator(paper.actionSource().actions()),
                new BinanceSpotTestnetExecutionService(
                        client,
                        portfolioService,
                        paper.execution().orderQuantity(),
                        paper.binance().recvWindowMillis(),
                        paper.execution().orderMode()));

        return new PaperBotRuntimeContext(runtime, paper, client.baseUrl());
    }

    private String resolveRequiredSecret(String environmentVariableName) {
        String value = environmentVariableResolver.get(environmentVariableName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + environmentVariableName);
        }
        return value;
    }
}
