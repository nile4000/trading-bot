package ch.lueem.tradingbot.modes.backtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import ch.lueem.tradingbot.adapters.config.BacktestConfig;
import ch.lueem.tradingbot.adapters.config.PortfolioConfig;
import ch.lueem.tradingbot.modes.backtest.model.BacktestPositionReport;
import ch.lueem.tradingbot.modes.backtest.model.BacktestReport;
import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;

class BacktestRunnerTest {

    @Test
    void run_exposesOpenPositionDetailsInReport() {
        BacktestRunner runner = new BacktestRunner();

        Path csvPath = Path.of("data/historical/BTCUSDT-1h.csv");
        StrategyDefinition strategy = new StrategyDefinition("ema_cross", new StrategyParameters(3, 7));
        BacktestConfig config = new BacktestConfig(
                csvPath,
                "BTCUSDT",
                "1h",
                strategy,
                new PortfolioConfig(10000.0));

        BacktestReport report = runner.run(config);
        BacktestPositionReport openPosition = report.positions().getLast();

        assertEquals(BotMode.BACKTEST, report.metadata().mode());
        assertTrue(report.hasOpenPosition());
        assertEquals("BTCUSDT", report.metadata().symbol());
        assertNotNull(report.finalValue());
        assertTrue(report.closedTradeCount() >= 0);
        assertFalse(report.positions().isEmpty());
        assertEquals("OPEN", openPosition.status());
        assertNotNull(openPosition.entryPrice());
        assertNotNull(openPosition.quantity());
        assertNotNull(openPosition.entryTime());
        assertTrue(openPosition.entryPrice().signum() > 0);
        assertTrue(openPosition.quantity().signum() > 0);
        assertFalse(openPosition.entryTime().isBlank());
        assertNull(openPosition.exitTime());
        assertNull(openPosition.exitPrice());
    }

    @Test
    void run_supportsSmaCrossStrategy() {
        BacktestRunner runner = new BacktestRunner();

        BacktestReport report = runner.run(new BacktestConfig(
                Path.of("data/historical/BTCUSDT-1h.csv"),
                "BTCUSDT",
                "1h",
                new StrategyDefinition("sma_cross", new StrategyParameters(3, 7)),
                new PortfolioConfig(10000.0)));

        assertEquals("sma_cross", report.metadata().strategy().name());
    }

    @Test
    void run_supportsRsiReversionStrategy() {
        BacktestRunner runner = new BacktestRunner();

        BacktestReport report = runner.run(new BacktestConfig(
                Path.of("data/historical/BTCUSDT-1h.csv"),
                "BTCUSDT",
                "1h",
                new StrategyDefinition("rsi_reversion", StrategyParameters.rsiReversion(5, 30, 70)),
                new PortfolioConfig(10000.0)));

        assertEquals("rsi_reversion", report.metadata().strategy().name());
    }
}
