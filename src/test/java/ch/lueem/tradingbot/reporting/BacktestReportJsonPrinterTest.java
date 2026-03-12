package ch.lueem.tradingbot.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import ch.lueem.tradingbot.application.BacktestRequest;
import ch.lueem.tradingbot.application.ReportingConfig;
import ch.lueem.tradingbot.backtest.BacktestReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BacktestReportJsonPrinterTest {

    @Test
    void print_includesPositionFieldsInJson() throws Exception {
        BacktestReportJsonPrinter printer = new BacktestReportJsonPrinter();
        BacktestRequest request = new BacktestRequest(
                Path.of("data/historical/BTCUSDT-1h.csv"),
                "ema_cross",
                "BTCUSDT",
                "1h",
                3,
                7,
                10000.0);
        BacktestReport report = new BacktestReport(
                "BTCUSDT",
                "1h",
                30,
                3,
                1,
                true,
                new BigDecimal("97198.3700"),
                new BigDecimal("0.1010"),
                "BTCUSDT",
                null,
                null,
                "2025-01-01T00:00:00Z",
                new BigDecimal("10000.0000"),
                new BigDecimal("9816.5138"),
                new BigDecimal("-1.8349"),
                new BigDecimal("0.0000"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        printer.print(new PrintStream(output), new ReportingConfig(false, true), request, report);

        JsonNode performance = new ObjectMapper()
                .readTree(output.toString(StandardCharsets.UTF_8))
                .get("performance");

        assertTrue(performance.get("openPosition").asBoolean());
        assertEquals(0, new BigDecimal("97198.3700").compareTo(performance.get("entryPrice").decimalValue()));
        assertEquals(0, new BigDecimal("0.1010").compareTo(performance.get("quantity").decimalValue()));
        assertEquals("BTCUSDT", performance.get("market").asText());
        assertTrue(performance.get("stopLoss").isNull());
        assertTrue(performance.get("takeProfit").isNull());
        assertEquals("2025-01-01T00:00:00Z", performance.get("openedAt").asText());
    }

    @Test
    void print_writesNullPositionFieldsWhenNoPositionIsOpen() throws Exception {
        BacktestReportJsonPrinter printer = new BacktestReportJsonPrinter();
        BacktestRequest request = new BacktestRequest(
                Path.of("data/historical/BTCUSDT-1h.csv"),
                "ema_cross",
                "BTCUSDT",
                "1h",
                3,
                7,
                10000.0);
        BacktestReport report = new BacktestReport(
                "BTCUSDT",
                "1h",
                30,
                2,
                1,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("10000.0000"),
                new BigDecimal("10010.0000"),
                new BigDecimal("0.1000"),
                new BigDecimal("100.0000"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        printer.print(new PrintStream(output), new ReportingConfig(false, true), request, report);

        JsonNode root = new ObjectMapper().readTree(output.toString(StandardCharsets.UTF_8));
        JsonNode performance = root.get("performance");

        assertFalse(performance.get("openPosition").asBoolean());
        assertTrue(performance.get("entryPrice").isNull());
        assertTrue(performance.get("quantity").isNull());
        assertTrue(performance.get("market").isNull());
        assertTrue(performance.get("stopLoss").isNull());
        assertTrue(performance.get("takeProfit").isNull());
        assertTrue(performance.get("openedAt").isNull());
        assertEquals(0, root.get("notes").size());
    }
}
