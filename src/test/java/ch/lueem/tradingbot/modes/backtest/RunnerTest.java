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
import ch.lueem.tradingbot.core.strategy.AdxFilterConfig;
import ch.lueem.tradingbot.adapters.config.backtest.PortfolioConfig;
import ch.lueem.tradingbot.adapters.market.CsvBarSeriesLoader;
import ch.lueem.tradingbot.modes.backtest.model.Report;
import ch.lueem.tradingbot.modes.backtest.model.Report.Position;
import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.strategy.StrategyEvaluatorContext;
import ch.lueem.tradingbot.core.strategy.StrategyEvaluatorFactory;
import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;

class RunnerTest {

    @Test
    void executesCloseSignalAtNextBarOpen() throws IOException {
        StrategyEvaluatorFactory signalOnFirstClose = new StrategyEvaluatorFactory() {
            @Override
            public StrategyActionEvaluator create(
                    StrategyDefinition definition,
                    StrategyEvaluatorContext context) {
                return actionContext -> actionContext.barIndex() == 0 ? TradeAction.BUY : TradeAction.HOLD;
            }
        };
        Runner runner = new Runner(new CsvBarSeriesLoader(), new ReportGenerator(), signalOnFirstClose);
        Path csvPath = writeHistoricalCsv(
                "timestamp,open,high,low,close,volume",
                "2026-01-01T00:00:00Z,90,100,90,100,1",
                "2026-01-01T01:00:00Z,120,130,120,130,1");

        Report report = runner.backtest(backtestConfig(
                csvPath,
                strategy("ema_cross", new StrategyParameters(3, 7))));

        assertEquals(new java.math.BigDecimal("120.0000"), report.positions().getFirst().entryPrice());
        assertEquals("signal_bar_close_next_bar_open", report.metadata().executionModel());
    }

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
                "2026-01-01T09:00:00Z,130,130,130,140,1",
                "2026-01-01T10:00:00Z,140,150,140,150,1");
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
        assertEquals(new java.math.BigDecimal("0.0001"), openPosition.quantity());
        assertEquals("fixed_quantity_spot", report.metadata().positionSizingModel());
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

    private BacktestConfig backtestConfig(Path csvPath, StrategyDefinition strategy) {
        return new BacktestConfig(
                csvPath,
                "BTCUSDT",
                "1h",
                strategy,
                new PortfolioConfig(new java.math.BigDecimal("10000.0")),
                new java.math.BigDecimal("0.0001"),
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                new AdxFilterConfig(false, 1, 0));
    }

    private StrategyDefinition strategy(String name, StrategyParameters parameters) {
        return new StrategyDefinition(name, parameters);
    }
}
