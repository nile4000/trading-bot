package ch.lueem.tradingbot.adapters.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.BacktestConfig;
import ch.lueem.tradingbot.adapters.config.PortfolioConfig;
import ch.lueem.tradingbot.adapters.config.ReportingConfig;
import ch.lueem.tradingbot.modes.backtest.model.BacktestMetadata;
import ch.lueem.tradingbot.modes.backtest.model.BacktestPositionReport;
import ch.lueem.tradingbot.modes.backtest.model.BacktestReport;
import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BacktestReportJsonPrinterTest {

    @Test
    void print_includesPositionFieldsInJson() throws Exception {
        BacktestReportJsonPrinter printer = new BacktestReportJsonPrinter();
        StrategyDefinition strategy = new StrategyDefinition("ema_cross", new StrategyParameters(3, 7));
        BacktestPositionReport openPosition = new BacktestPositionReport(
                1,
                "OPEN",
                "2025-01-01T00:00:00Z",
                new BigDecimal("97198.3700"),
                null,
                null,
                new BigDecimal("0.1010"),
                new BigDecimal("-183.4862"),
                new BigDecimal("-1.8349"));
        BacktestConfig config = new BacktestConfig(
                Path.of("data/historical/BTCUSDT-1h.csv"),
                "BTCUSDT",
                "1h",
                strategy,
                new PortfolioConfig(10000.0));
        BacktestReport report = new BacktestReport(
                new BacktestMetadata(
                        BotMode.BACKTEST,
                        "BTCUSDT",
                        "1h",
                        30,
                        "2025-01-01T00:00:00Z",
                        "2025-01-02T00:00:00Z",
                        "action_bar_close",
                        "all_in_spot",
                        strategy),
                3,
                1,
                true,
                new BigDecimal("10000.0000"),
                new BigDecimal("9816.5138"),
                new BigDecimal("-1.8349"),
                new BigDecimal("0.0000"),
                List.of(openPosition));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        printer.print(new PrintStream(output), new ReportingConfig(false, true), config, report);

        JsonNode root = new ObjectMapper()
                .readTree(output.toString(StandardCharsets.UTF_8))
                ;
        JsonNode metadata = root.get("metadata");
        JsonNode performance = root.get("performance");
        JsonNode positions = root.get("positions");
        JsonNode notes = root.get("notes");

        assertEquals("v4", root.get("reportVersion").asText());
        assertEquals("BTCUSDT", metadata.get("symbol").asText());
        assertEquals("action_bar_close", metadata.get("executionModel").asText());
        assertEquals("ema_cross", root.get("strategy").get("name").asText());
        assertEquals(3, root.get("strategy").get("parameters").get("shortEma").asInt());
        assertEquals(7, root.get("strategy").get("parameters").get("longEma").asInt());
        assertTrue(performance.get("hasOpenPosition").asBoolean());
        assertEquals(1, positions.size());
        assertEquals("OPEN", positions.get(0).get("status").asText());
        assertEquals(0, new BigDecimal("97198.3700").compareTo(positions.get(0).get("entryPrice").decimalValue()));
        assertEquals(0, new BigDecimal("0.1010").compareTo(positions.get(0).get("quantity").decimalValue()));
        assertTrue(positions.get(0).get("exitTime").isNull());
        assertTrue(positions.get(0).get("exitPrice").isNull());
        assertEquals(1, notes.size());
    }

    @Test
    void print_writesNullPositionFieldsWhenNoPositionIsOpen() throws Exception {
        BacktestReportJsonPrinter printer = new BacktestReportJsonPrinter();
        StrategyDefinition strategy = new StrategyDefinition("ema_cross", new StrategyParameters(3, 7));
        BacktestConfig config = new BacktestConfig(
                Path.of("data/historical/BTCUSDT-1h.csv"),
                "BTCUSDT",
                "1h",
                strategy,
                new PortfolioConfig(10000.0));
        BacktestReport report = new BacktestReport(
                new BacktestMetadata(
                        BotMode.BACKTEST,
                        "BTCUSDT",
                        "1h",
                        30,
                        "2025-01-01T00:00:00Z",
                        "2025-01-02T00:00:00Z",
                        "action_bar_close",
                        "all_in_spot",
                        strategy),
                2,
                1,
                false,
                new BigDecimal("10000.0000"),
                new BigDecimal("10010.0000"),
                new BigDecimal("0.1000"),
                new BigDecimal("100.0000"),
                List.of());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        printer.print(new PrintStream(output), new ReportingConfig(false, true), config, report);

        JsonNode root = new ObjectMapper().readTree(output.toString(StandardCharsets.UTF_8));
        JsonNode performance = root.get("performance");

        assertFalse(performance.get("hasOpenPosition").asBoolean());
        assertEquals(0, root.get("positions").size());
        assertEquals(0, root.get("notes").size());
    }
}
