package ch.lueem.tradingbot.adapters.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.ReportingConfig;
import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.adapters.config.backtest.PortfolioConfig;
import ch.lueem.tradingbot.modes.backtest.model.Metadata;
import ch.lueem.tradingbot.modes.backtest.model.Report;
import ch.lueem.tradingbot.modes.backtest.model.Report.Position;
import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BacktestReportJsonPrinterTest {

    @Test
    void print_omitsPositionsFromJson() throws Exception {
        BacktestReportJsonPrinter printer = new BacktestReportJsonPrinter();
        StrategyDefinition strategy = new StrategyDefinition("ema_cross", new StrategyParameters(3, 7));
        Position openPosition = new Position(
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
        Report report = new Report(
                new Metadata(
                        BotMode.BACKTEST,
                        "BTCUSDT",
                        "1h",
                        30,
                        "2025-01-01T00:00:00Z",
                        "2025-01-02T00:00:00Z",
                        "action_bar_close",
                        "all_in_spot",
                        strategy),
                1,
                new BigDecimal("10000.0000"),
                new BigDecimal("9816.5138"),
                new BigDecimal("-1.8349"),
                new BigDecimal("8.2500"),
                new BigDecimal("12.5000"),
                new BigDecimal("0.0000"),
                new BigDecimal("0.0000"),
                new BigDecimal("0.0000"),
                new BigDecimal("0.0000"),
                new BigDecimal("0.5000"),
                new BigDecimal("40.0000"),
                true,
                List.of(openPosition));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        printer.print(new PrintStream(output), new ReportingConfig(false, true), config, report);

        JsonNode root = new ObjectMapper()
                .readTree(output.toString(StandardCharsets.UTF_8))
                ;
        JsonNode metadata = root.get("metadata");
        JsonNode performance = root.get("performance");
        JsonNode notes = root.get("notes");

        assertEquals("v5", root.get("reportVersion").asText());
        assertEquals("BTCUSDT", metadata.get("symbol").asText());
        assertEquals("action_bar_close", metadata.get("executionModel").asText());
        assertEquals("ema_cross", root.get("strategy").get("name").asText());
        assertEquals(3, root.get("strategy").get("parameters").get("shortEma").asInt());
        assertEquals(7, root.get("strategy").get("parameters").get("longEma").asInt());
        assertEquals(1, performance.get("closedTradeCount").asInt());
        assertEquals(0, new BigDecimal("-1.8349").compareTo(performance.get("totalReturnPercent").decimalValue()));
        assertEquals(0, new BigDecimal("8.2500").compareTo(performance.get("buyAndHoldReturnPercent").decimalValue()));
        assertEquals(0, new BigDecimal("12.5000").compareTo(performance.get("maxDrawdownPercent").decimalValue()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(performance.get("profitFactor").decimalValue()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(performance.get("averageWinningTrade").decimalValue()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(performance.get("averageLosingTrade").decimalValue()));
        assertEquals(0, new BigDecimal("0.5000").compareTo(performance.get("timeInMarketDays").decimalValue()));
        assertEquals(0, new BigDecimal("40.0000").compareTo(performance.get("exposurePercent").decimalValue()));
        assertFalse(root.has("positions"));
        assertEquals(2, notes.size());
    }

    @Test
    void print_usesCompactPerformanceSchemaWhenNoPositionIsOpen() throws Exception {
        BacktestReportJsonPrinter printer = new BacktestReportJsonPrinter();
        StrategyDefinition strategy = new StrategyDefinition("ema_cross", new StrategyParameters(3, 7));
        BacktestConfig config = new BacktestConfig(
                Path.of("data/historical/BTCUSDT-1h.csv"),
                "BTCUSDT",
                "1h",
                strategy,
                new PortfolioConfig(10000.0));
        Report report = new Report(
                new Metadata(
                        BotMode.BACKTEST,
                        "BTCUSDT",
                        "1h",
                        30,
                        "2025-01-01T00:00:00Z",
                        "2025-01-02T00:00:00Z",
                        "action_bar_close",
                        "all_in_spot",
                        strategy),
                1,
                new BigDecimal("10000.0000"),
                new BigDecimal("10010.0000"),
                new BigDecimal("0.1000"),
                new BigDecimal("1.5000"),
                new BigDecimal("3.2500"),
                new BigDecimal("1.8000"),
                new BigDecimal("100.0000"),
                new BigDecimal("100.0000"),
                new BigDecimal("0.0000"),
                new BigDecimal("0.0000"),
                new BigDecimal("0.0000"),
                false,
                List.of());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        printer.print(new PrintStream(output), new ReportingConfig(false, true), config, report);

        JsonNode root = new ObjectMapper().readTree(output.toString(StandardCharsets.UTF_8));
        JsonNode performance = root.get("performance");

        assertEquals(1, performance.get("closedTradeCount").asInt());
        assertEquals(0, new BigDecimal("1.5000").compareTo(performance.get("buyAndHoldReturnPercent").decimalValue()));
        assertEquals(0, new BigDecimal("3.2500").compareTo(performance.get("maxDrawdownPercent").decimalValue()));
        assertEquals(0, new BigDecimal("1.8000").compareTo(performance.get("profitFactor").decimalValue()));
        assertEquals(0, new BigDecimal("100.0000").compareTo(performance.get("averageWinningTrade").decimalValue()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(performance.get("averageLosingTrade").decimalValue()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(performance.get("timeInMarketDays").decimalValue()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(performance.get("exposurePercent").decimalValue()));
        assertFalse(root.has("positions"));
        assertEquals(1, root.get("notes").size());
    }
}
