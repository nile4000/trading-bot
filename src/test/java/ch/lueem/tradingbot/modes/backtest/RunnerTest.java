package ch.lueem.tradingbot.modes.backtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.adapters.config.backtest.PortfolioConfig;
import ch.lueem.tradingbot.modes.backtest.model.Report;
import ch.lueem.tradingbot.modes.backtest.model.Report.Position;
import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;

class RunnerTest {

    @Test
    void run_exposesOpenPositionDetailsInReport() throws IOException {
        Runner runner = new Runner();

        Path csvPath = writeHistoricalCsv(
                "timestamp,open,high,low,close,volume",
                "2026-01-01T00:00:00Z,100,100,100,100,1",
                "2026-01-01T01:00:00Z,100,100,100,100,1",
                "2026-01-01T02:00:00Z,100,100,100,100,1",
                "2026-01-01T03:00:00Z,100,100,100,100,1",
                "2026-01-01T04:00:00Z,100,100,100,100,1",
                "2026-01-01T05:00:00Z,100,100,100,100,1",
                "2026-01-01T06:00:00Z,100,100,100,110,1",
                "2026-01-01T07:00:00Z,110,110,110,120,1",
                "2026-01-01T08:00:00Z,120,120,120,130,1",
                "2026-01-01T09:00:00Z,130,130,130,140,1");
        BacktestConfig config = backtestConfig(csvPath, strategy("ema_cross", new StrategyParameters(3, 7)));

        Report report = runner.backtest(config);
        Position openPosition = report.positions().getLast();

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

    private Path writeHistoricalCsv(String... lines) throws IOException {
        Path csvFile = Files.createTempFile("backtest-open-position-", ".csv");
        Files.writeString(csvFile, String.join(System.lineSeparator(), lines), StandardCharsets.UTF_8);
        csvFile.toFile().deleteOnExit();
        return csvFile;
    }

    @Test
    void run_supportsSmaCrossStrategy() {
        Runner runner = new Runner();

        Report report = runner.backtest(backtestConfig(
                Path.of("data/historical/BTCUSDT-1h.csv"),
                strategy("sma_cross", new StrategyParameters(3, 7))));

        assertEquals("sma_cross", report.metadata().strategy().name());
    }

    @Test
    void run_supportsRsiReversionStrategy() {
        Runner runner = new Runner();

        Report report = runner.backtest(backtestConfig(
                Path.of("data/historical/BTCUSDT-1h.csv"),
                strategy("rsi_reversion", StrategyParameters.rsiReversion(5, 30, 70))));

        assertEquals("rsi_reversion", report.metadata().strategy().name());
    }

    private BacktestConfig backtestConfig(Path csvPath, StrategyDefinition strategy) {
        return new BacktestConfig(
                csvPath,
                "BTCUSDT",
                "1h",
                strategy,
                new PortfolioConfig(10000.0));
    }

    private StrategyDefinition strategy(String name, StrategyParameters parameters) {
        return new StrategyDefinition(name, parameters);
    }
}
