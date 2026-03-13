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
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
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
