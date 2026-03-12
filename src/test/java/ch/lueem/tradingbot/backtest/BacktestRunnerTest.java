package ch.lueem.tradingbot.backtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class BacktestRunnerTest {

    @Test
    void run_exposesOpenPositionDetailsInReport() {
        BacktestRunner runner = new BacktestRunner();

        BacktestReport report = runner.run(
                Path.of("data/historical/BTCUSDT-1h.csv"),
                "BTCUSDT",
                "1h",
                3,
                7,
                10000.0);

        assertTrue(report.openPosition());
        assertEquals("BTCUSDT", report.market());
        assertNotNull(report.entryPrice());
        assertNotNull(report.quantity());
        assertNotNull(report.openedAt());
        assertTrue(report.entryPrice().signum() > 0);
        assertTrue(report.quantity().signum() > 0);
        assertFalse(report.openedAt().isBlank());
        assertNull(report.stopLoss());
        assertNull(report.takeProfit());
    }
}
