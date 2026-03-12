package ch.lueem.tradingbot.backtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import ch.lueem.tradingbot.application.BacktestRequest;
import ch.lueem.tradingbot.backtest.model.BacktestPositionReport;
import ch.lueem.tradingbot.backtest.model.BacktestReport;
import ch.lueem.tradingbot.bot.model.BotMode;
import ch.lueem.tradingbot.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;

class BacktestRunnerTest {

    @Test
    void run_exposesOpenPositionDetailsInReport() {
        BacktestRunner runner = new BacktestRunner();

        Path csvPath = Path.of("data/historical/BTCUSDT-1h.csv");
        StrategyDefinition strategy = new StrategyDefinition("ema_cross", new StrategyParameters(3, 7));
        BacktestRequest request = new BacktestRequest(
                csvPath,
                "BTCUSDT",
                "1h",
                strategy,
                10000.0);

        BacktestReport report = runner.run(request);
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
}
