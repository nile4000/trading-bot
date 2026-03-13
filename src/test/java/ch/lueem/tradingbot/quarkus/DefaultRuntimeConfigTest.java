package ch.lueem.tradingbot.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.lueem.tradingbot.adapters.config.ReportingConfig;
import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DefaultRuntimeConfigTest {

    @Inject
    BacktestConfig backtestConfig;
    @Inject
    PaperConfig paperConfig;
    @Inject
    ReportingConfig reportingConfig;
    @Inject
    TradingBotRuntimeConfig runtimeConfig;

    @Test
    void loadsDefaultBacktestConfiguration() {
        assertEquals("BTCUSDT", backtestConfig.symbol());
        assertEquals("1h", backtestConfig.timeframe());
        assertEquals("data/historical/BTCUSDT-1h.csv", backtestConfig.csvPath().toString().replace('\\', '/'));
        assertEquals(3, backtestConfig.strategy().parameters().shortEma());
        assertEquals(7, backtestConfig.strategy().parameters().longEma());
    }

    @Test
    void loadsSharedRuntimeConfiguration() {
        assertFalse(runtimeConfig.app().lifecycleEvents());
        assertNotNull(paperConfig);
        assertEquals("ema_cross", paperConfig.strategy().name());
        assertEquals("https://testnet.binance.vision", runtimeConfig.paper().binance().baseUrl());
        assertEquals(true, reportingConfig.prettyPrint());
    }
}
