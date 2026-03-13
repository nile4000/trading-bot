package ch.lueem.tradingbot.quarkus;

import java.nio.file.Path;

import ch.lueem.tradingbot.adapters.config.ReportingConfig;
import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.adapters.config.backtest.PortfolioConfig;
import ch.lueem.tradingbot.adapters.config.paper.BinanceConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperBotConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperExecutionConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperStrategyConfig;
import ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClientFactory;
import ch.lueem.tradingbot.adapters.market.CsvBarSeriesLoader;
import ch.lueem.tradingbot.adapters.reporting.BacktestReportJsonPrinter;
import ch.lueem.tradingbot.core.strategy.StrategyEvaluatorFactory;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import ch.lueem.tradingbot.modes.backtest.BacktestService;
import ch.lueem.tradingbot.modes.backtest.ReportGenerator;
import ch.lueem.tradingbot.modes.backtest.Runner;
import ch.lueem.tradingbot.modes.paper.PaperBotLoop;
import ch.lueem.tradingbot.modes.paper.PaperBotService;
import ch.lueem.tradingbot.modes.paper.PaperBotSetup;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Produces the runtime configuration objects and services hosted by Quarkus.
 */
@Singleton
public class QuarkusRuntimeProducer {

    @Produces
    @Singleton
    ReportingConfig reportingConfig(TradingBotRuntimeConfig config) {
        return new ReportingConfig(config.reporting().prettyPrint(), config.reporting().includeNotes());
    }

    @Produces
    @Singleton
    BacktestConfig backtestConfig(TradingBotRuntimeConfig config) {
        var backtest = config.backtest();
        return new BacktestConfig(
                Path.of(backtest.csvPath()),
                backtest.symbol(),
                backtest.timeframe(),
                toStrategyDefinition(backtest.strategy()),
                new PortfolioConfig(backtest.portfolio().initialCash()));
    }

    @Produces
    @Singleton
    PaperConfig paperConfig(TradingBotRuntimeConfig config) {
        var paper = config.paper();
        return new PaperConfig(
                new PaperBotConfig(
                        paper.bot().botId(),
                        paper.bot().botVersion(),
                        paper.bot().symbol(),
                        paper.bot().timeframe()),
                new PaperExecutionConfig(
                        paper.execution().exchange(),
                        paper.execution().orderMode(),
                        paper.execution().tickIntervalMillis(),
                        paper.execution().initialCash(),
                        paper.execution().orderQuantity(),
                        paper.execution().placeOrdersEnabled(),
                        paper.execution().maxOrderNotional().orElse(null)),
                new PaperStrategyConfig(
                        paper.strategy().name(),
                        toStrategyParameters(paper.strategy().parameters().orElse(null)),
                        paper.strategy().actions().orElse(null)),
                new BinanceConfig(
                        paper.binance().apiKey().orElse(null),
                        paper.binance().secretKey().orElse(null),
                        paper.binance().recvWindowMillis()));
    }

    @Produces
    @Singleton
    BinanceClientFactory binanceClientFactory(TradingBotRuntimeConfig config) {
        return new BinanceClientFactory(config.paper().binance().baseUrl());
    }

    @Produces
    @Singleton
    StrategyEvaluatorFactory strategyEvaluatorFactory() {
        return new StrategyEvaluatorFactory();
    }

    @Produces
    @Singleton
    CsvBarSeriesLoader csvBarSeriesLoader() {
        return new CsvBarSeriesLoader();
    }

    @Produces
    @Singleton
    ReportGenerator reportGenerator() {
        return new ReportGenerator();
    }

    @Produces
    @Singleton
    Runner backtestRunner(
            CsvBarSeriesLoader csvBarSeriesLoader,
            ReportGenerator reportGenerator,
            StrategyEvaluatorFactory strategyEvaluatorFactory) {
        return new Runner(csvBarSeriesLoader, reportGenerator, strategyEvaluatorFactory);
    }

    @Produces
    @Singleton
    BacktestReportJsonPrinter backtestReportJsonPrinter() {
        return new BacktestReportJsonPrinter();
    }

    @Produces
    @Singleton
    BacktestService backtestService(Runner backtestRunner, BacktestReportJsonPrinter reportPrinter) {
        return new BacktestService(backtestRunner, reportPrinter);
    }

    @Produces
    @Singleton
    PaperBotSetup paperBotSetup(
            BinanceClientFactory clientFactory,
            StrategyEvaluatorFactory strategyEvaluatorFactory) {
        return new PaperBotSetup(clientFactory, strategyEvaluatorFactory);
    }

    @Produces
    @Singleton
    PaperBotLoop paperBotLoop() {
        return new PaperBotLoop();
    }

    @Produces
    @Singleton
    PaperBotService paperBotService(PaperBotSetup setup, PaperBotLoop loop) {
        return new PaperBotService(setup, loop);
    }

    private StrategyDefinition toStrategyDefinition(TradingBotRuntimeConfig.Strategy strategy) {
        return new StrategyDefinition(strategy.name(), toStrategyParameters(strategy.parameters().orElse(null)));
    }

    private StrategyParameters toStrategyParameters(TradingBotRuntimeConfig.StrategyParameters parameters) {
        if (parameters == null) {
            return null;
        }
        return new StrategyParameters(
                parameters.shortEma().orElse(null),
                parameters.longEma().orElse(null),
                parameters.rsiPeriod().orElse(null),
                parameters.buyBelow().orElse(null),
                parameters.sellAbove().orElse(null));
    }
}
