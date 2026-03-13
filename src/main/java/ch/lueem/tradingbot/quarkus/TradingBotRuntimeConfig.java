package ch.lueem.tradingbot.quarkus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import ch.lueem.tradingbot.adapters.config.paper.PaperExchange;
import ch.lueem.tradingbot.adapters.config.paper.PaperOrderMode;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Maps the Quarkus runtime configuration for the supported trading bot flows.
 */
@ConfigMapping(prefix = "trading")
public interface TradingBotRuntimeConfig {

    App app();
    Reporting reporting();
    Backtest backtest();
    Paper paper();

    interface App {
        @WithDefault("false")
        boolean lifecycleEvents();
    }

    interface Reporting {
        @WithDefault("true")
        boolean prettyPrint();

        @WithDefault("true")
        boolean includeNotes();
    }

    interface Backtest {
        @NotBlank
        String csvPath();

        @NotBlank
        String symbol();

        @NotBlank
        String timeframe();

        Strategy strategy();

        Portfolio portfolio();
    }

    interface Portfolio {
        @Positive
        double initialCash();
    }

    interface Paper {
        Bot bot();

        Execution execution();

        Strategy strategy();

        Binance binance();
    }

    interface Bot {
        @NotBlank
        String botId();

        @NotBlank
        String botVersion();

        @NotBlank
        String symbol();

        @NotBlank
        String timeframe();
    }

    interface Execution {
        PaperExchange exchange();

        PaperOrderMode orderMode();

        @Positive
        long tickIntervalMillis();

        @Positive
        double initialCash();

        @Positive
        BigDecimal orderQuantity();

        @WithDefault("false")
        boolean placeOrdersEnabled();

        Optional<BigDecimal> maxOrderNotional();
    }

    interface Binance {
        Optional<String> apiKey();

        Optional<String> secretKey();

        @Positive
        double recvWindowMillis();

        @NotBlank
        String baseUrl();
    }

    interface Strategy {
        @NotBlank
        String name();

        Optional<StrategyParameters> parameters();

        Optional<List<TradeAction>> actions();
    }

    interface StrategyParameters {
        Optional<Integer> shortEma();

        Optional<Integer> longEma();

        Optional<Integer> rsiPeriod();

        Optional<Integer> buyBelow();

        Optional<Integer> sellAbove();
    }
}
