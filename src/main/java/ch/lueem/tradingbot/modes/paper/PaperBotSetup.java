package ch.lueem.tradingbot.modes.paper;

import java.math.BigDecimal;

import ch.lueem.tradingbot.adapters.config.PaperConfig;
import ch.lueem.tradingbot.adapters.config.PaperExchange;
import ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClient;
import ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClientFactory;
import ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetExecutionService;
import ch.lueem.tradingbot.adapters.market.BinanceTickerPriceMarketSnapshotProvider;
import ch.lueem.tradingbot.adapters.portfolio.PaperPortfolioService;
import ch.lueem.tradingbot.core.runtime.TradingRuntime;
import ch.lueem.tradingbot.core.strategy.action.QueuedActionEvaluator;

/**
 * Resolves secrets and infrastructure dependencies for the configured paper bot
 * session.
 */
public class PaperBotSetup {

    private final BinanceSpotTestnetClientFactory clientFactory;
    private final EnvironmentVariableResolver environmentVariableResolver;

    public PaperBotSetup(
            BinanceSpotTestnetClientFactory clientFactory,
            EnvironmentVariableResolver environmentVariableResolver) {
        this.clientFactory = clientFactory;
        this.environmentVariableResolver = environmentVariableResolver;
    }

    public PaperBotSession createSession(PaperConfig paper) {
        paper.validate();
        ensureSupportedExchange(paper);

        BinanceSpotTestnetClient client = createClient(paper);
        PaperPortfolioService portfolioService = createPortfolioService(paper);
        TradingRuntime runtime = createRuntime(paper, client, portfolioService);
        return new PaperBotSession(runtime, paper, client.baseUrl());
    }

    private String resolveRequiredSecret(String environmentVariableName) {
        String value = environmentVariableResolver.get(environmentVariableName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + environmentVariableName);
        }
        return value;
    }

    private void ensureSupportedExchange(PaperConfig paper) {
        if (paper.execution().exchange() != PaperExchange.BINANCE_SPOT_TESTNET) {
            throw new IllegalStateException("Unsupported paper exchange in phase 1: " + paper.execution().exchange());
        }
    }

    private BinanceSpotTestnetClient createClient(PaperConfig paper) {
        return clientFactory.create(
                resolveRequiredSecret(paper.binance().apiKeyEnv()),
                resolveRequiredSecret(paper.binance().secretKeyEnv()));
    }

    private PaperPortfolioService createPortfolioService(PaperConfig paper) {
        return new PaperPortfolioService(
                paper.bot().symbol(),
                BigDecimal.valueOf(paper.execution().initialCash()));
    }

    private TradingRuntime createRuntime(
            PaperConfig paper,
            BinanceSpotTestnetClient client,
            PaperPortfolioService portfolioService) {
        return new TradingRuntime(
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
    }
}
