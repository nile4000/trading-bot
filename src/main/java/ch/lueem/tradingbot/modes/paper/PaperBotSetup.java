package ch.lueem.tradingbot.modes.paper;

import java.math.BigDecimal;

import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperExchange;
import ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClient;
import ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClientFactory;
import ch.lueem.tradingbot.adapters.execution.binance.flow.BinancePaperExecutionService;
import ch.lueem.tradingbot.adapters.market.BinanceTickerPriceMarketSnapshotProvider;
import ch.lueem.tradingbot.adapters.portfolio.PaperPortfolioService;
import ch.lueem.tradingbot.core.runtime.TradingRuntime;
import ch.lueem.tradingbot.core.strategy.StrategyEvaluatorContext;
import ch.lueem.tradingbot.core.strategy.StrategyEvaluatorFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Resolves secrets and infrastructure dependencies for the configured paper bot
 * session.
 */
@Singleton
public class PaperBotSetup {

    private final BinanceClientFactory clientFactory;
    private final StrategyEvaluatorFactory strategyFactory;

    public PaperBotSetup(BinanceClientFactory clientFactory) {
        this(clientFactory, new StrategyEvaluatorFactory());
    }

    @Inject
    public PaperBotSetup(
            BinanceClientFactory clientFactory,
            StrategyEvaluatorFactory strategyFactory) {
        this.clientFactory = clientFactory;
        this.strategyFactory = strategyFactory;
    }

    public PaperBotSession createSession(PaperConfig paper) {
        ensureSupportedExchange(paper);

        var client = createClient(paper);
        var marketSnapshotProvider = new BinanceTickerPriceMarketSnapshotProvider(client);
        var portfolioService = createPortfolioService(paper);
        var runtime = createRuntime(paper, client, marketSnapshotProvider, portfolioService);
        return new PaperBotSession(runtime, paper, client.baseUrl());
    }

    private String requireSecret(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration value: " + propertyName);
        }
        return value;
    }

    private void ensureSupportedExchange(PaperConfig paper) {
        if (paper.execution().exchange() != PaperExchange.BINANCE_SPOT_TESTNET) {
            throw new IllegalStateException("Unsupported paper exchange in phase 1: " + paper.execution().exchange());
        }
    }

    private BinanceClient createClient(PaperConfig paper) {
        return clientFactory.create(
                requireSecret(paper.binance().apiKey(), "paper.binance.apiKey"),
                requireSecret(paper.binance().secretKey(), "paper.binance.secretKey"));
    }

    private PaperPortfolioService createPortfolioService(PaperConfig paper) {
        return new PaperPortfolioService(
                paper.bot().symbol(),
                BigDecimal.valueOf(paper.execution().initialCash()));
    }

    private TradingRuntime createRuntime(
            PaperConfig paper,
            BinanceClient client,
            BinanceTickerPriceMarketSnapshotProvider marketSnapshotProvider,
            PaperPortfolioService portfolioService) {
        var evaluator = strategyFactory.create(
                paper.strategy().toStrategyDefinition(),
                StrategyEvaluatorContext.ta4jOrQueued(marketSnapshotProvider.series(), paper.strategy().actions()));
        return new TradingRuntime(
                paper.toTradingDefinition(),
                marketSnapshotProvider,
                portfolioService,
                evaluator,
                new BinancePaperExecutionService(
                        client,
                        portfolioService,
                        paper.execution().orderQuantity(),
                        paper.binance().recvWindowMillis(),
                        paper.execution().orderMode(),
                        paper.execution().placeOrdersEnabled(),
                        paper.execution().maxOrderNotional()));
    }
}
