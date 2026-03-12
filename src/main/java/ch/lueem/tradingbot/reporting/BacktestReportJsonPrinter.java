package ch.lueem.tradingbot.reporting;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.application.BacktestRequest;
import ch.lueem.tradingbot.backtest.BacktestPositionReport;
import ch.lueem.tradingbot.application.ReportingConfig;
import ch.lueem.tradingbot.backtest.BacktestReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Converts a backtest result into a stable JSON document and writes it to an output stream.
 */
public class BacktestReportJsonPrinter {

    private static final String REPORT_VERSION = "v2";
    private final ObjectMapper prettyObjectMapper;
    private final ObjectMapper compactObjectMapper;

    public BacktestReportJsonPrinter() {
        this.prettyObjectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.compactObjectMapper = new ObjectMapper();
    }

    public void print(
            PrintStream out,
            ReportingConfig reporting,
            BacktestRequest request,
            BacktestReport report) {
        if (out == null) {
            throw new IllegalArgumentException("out must not be null.");
        }
        if (reporting == null) {
            throw new IllegalArgumentException("reporting must not be null.");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null.");
        }
        if (report == null) {
            throw new IllegalArgumentException("report must not be null.");
        }

        BacktestReportDocument document = new BacktestReportDocument(
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                REPORT_VERSION,
                new MetadataSection(
                        request.csvPath().toString(),
                        report.metadata().symbol(),
                        report.metadata().timeframe(),
                        report.metadata().barCount(),
                        report.metadata().dataStart(),
                        report.metadata().dataEnd(),
                        report.metadata().executionModel(),
                        report.metadata().positionSizingModel()),
                new StrategySection(
                        report.metadata().strategy().name(),
                        report.metadata().strategy().parameters().shortEma(),
                        report.metadata().strategy().parameters().longEma()),
                new PerformanceSection(
                        report.signalCount(),
                        report.tradeCount(),
                        report.openPosition(),
                        report.initialCash(),
                        report.finalValue(),
                        report.returnPct(),
                        report.winRatePct()),
                report.positions(),
                buildNotes(report, reporting));

        out.println(toJson(document, reporting));
    }

    private List<String> buildNotes(BacktestReport report, ReportingConfig reporting) {
        List<String> notes = new ArrayList<>();
        if (!reporting.includeNotes()) {
            return notes;
        }
        if (report.tradeCount() == 0) {
            notes.add("No closed EMA cross trade was generated on this dataset.");
        }
        if (report.openPosition()) {
            notes.add("An open position remains at the end of the series and is valued mark-to-market.");
        }
        return notes;
    }

    private String toJson(BacktestReportDocument document, ReportingConfig reporting) {
        try {
            return selectObjectMapper(reporting.prettyPrint()).writeValueAsString(document);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to render backtest report as JSON.", exception);
        }
    }

    private ObjectMapper selectObjectMapper(boolean prettyPrint) {
        return prettyPrint ? prettyObjectMapper : compactObjectMapper;
    }

    private record BacktestReportDocument(
            String timestampUtc,
            String reportVersion,
            MetadataSection metadata,
            StrategySection strategy,
            PerformanceSection performance,
            List<BacktestPositionReport> positions,
            List<String> notes) {
    }

    private record MetadataSection(
            String csvPath,
            String symbol,
            String timeframe,
            int barCount,
            String dataStart,
            String dataEnd,
            String executionModel,
            String positionSizingModel) {
    }

    private record StrategySection(
            String name,
            int shortEma,
            int longEma) {
    }

    private record PerformanceSection(
            int signalCount,
            int tradeCount,
            boolean openPosition,
            BigDecimal initialCash,
            BigDecimal finalValue,
            BigDecimal returnPct,
            BigDecimal winRatePct) {
    }
}
