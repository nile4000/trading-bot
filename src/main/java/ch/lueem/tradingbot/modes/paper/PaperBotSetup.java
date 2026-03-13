package ch.lueem.tradingbot.modes.paper;

import java.math.BigDecimal;

import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClient;
import ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClientFactory;
import ch.lueem.tradingbot.adapters.execution.binance.flow.BinancePaperExecutionService;
import ch.lueem.tradingbot.adapters.market.BinancePriceSnapshotProvider;
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
        var client = createClient(paper);
        var marketSnapshotProvider = new BinancePriceSnapshotProvider(client);
        var portfolioService = createPortfolioService(paper);
        var runtime = createRuntime(paper, client, marketSnapshotProvider, portfolioService);
        return new PaperBotSession(runtime, paper, client.baseUrl());
    }

    private BinanceClient createClient(PaperConfig paper) {
        return clientFactory.create(paper.binance().apiKey(), paper.binance().secretKey());
    }

    private PaperPortfolioService createPortfolioService(PaperConfig paper) {
        return new PaperPortfolioService(
                paper.bot().symbol(),
                BigDecimal.valueOf(paper.execution().initialCash()));
    }

    private TradingRuntime createRuntime(
            PaperConfig paper,
            BinanceClient client,
            BinancePriceSnapshotProvider marketSnapshotProvider,
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
