package ch.lueem.tradingbot.modes.paper;

import java.math.BigDecimal;

import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperOrderMode;
import ch.lueem.tradingbot.adapters.binance.client.BinanceClient;
import ch.lueem.tradingbot.adapters.binance.client.BinanceClientFactory;
import ch.lueem.tradingbot.adapters.execution.binance.flow.BinancePaperExecutionService;
import ch.lueem.tradingbot.adapters.execution.binance.flow.BinancePortfolioSync;
import ch.lueem.tradingbot.adapters.execution.binance.order.BinanceClientOrderId;
import ch.lueem.tradingbot.adapters.market.BinanceKlineSnapshotProvider;
import ch.lueem.tradingbot.adapters.portfolio.PaperPortfolioService;
import ch.lueem.tradingbot.core.execution.ExecutionService;
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
        var definition = paper.toTradingDefinition();
        var marketSnapshotProvider = new BinanceKlineSnapshotProvider(client);
        marketSnapshotProvider.initialize(
                definition,
                Math.max(1, strategyFactory.requiredHistoryBars(definition.strategy())));
        var portfolioService = createPortfolioService(paper);
        var evaluator = strategyFactory.create(
                paper.strategy().toStrategyDefinition(),
                StrategyEvaluatorContext.ta4jOrQueued(
                        marketSnapshotProvider.series(), paper.strategy().actions()));
        var executionService = createExecutionService(paper, client, portfolioService);
        var runtime = new TradingRuntime(
                definition,
                marketSnapshotProvider,
                portfolioService,
                evaluator,
                executionService);
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

    private ExecutionService createExecutionService(
            PaperConfig paper,
            BinanceClient client,
            PaperPortfolioService portfolioService) {
        if (paper.execution().orderMode() == PaperOrderMode.VALIDATE_ONLY) {
            return new BinancePaperExecutionService(
                    client,
                    portfolioService,
                    paper.execution().orderQuantity(),
                    paper.binance().recvWindowMillis(),
                    paper.execution().orderMode(),
                    paper.execution().placeOrdersEnabled(),
                    paper.execution().maxOrderNotional());
        }

        var clientOrderId = new BinanceClientOrderId();
        var symbolInfo = client.loadSymbolInfo(paper.bot().symbol());
        var portfolioSync = new BinancePortfolioSync(
                client,
                portfolioService,
                paper,
                symbolInfo,
                clientOrderId);
        portfolioSync.sync();
        return new BinancePaperExecutionService(
                client,
                portfolioService,
                paper.execution().orderQuantity(),
                paper.binance().recvWindowMillis(),
                paper.execution().orderMode(),
                paper.execution().placeOrdersEnabled(),
                paper.execution().maxOrderNotional(),
                portfolioSync,
                clientOrderId,
                symbolInfo);
    }
}
