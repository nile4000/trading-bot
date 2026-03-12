package ch.lueem.tradingbot.reporting;

import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.application.BacktestRequest;
import ch.lueem.tradingbot.application.ReportingConfig;
import ch.lueem.tradingbot.backtest.BacktestReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Converts a backtest result into a stable JSON document and writes it to an output stream.
 */
public class BacktestReportJsonPrinter {

    private static final String REPORT_VERSION = "v1";
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
                new DatasetSection(request.csvPath().toString(), report.symbol(), report.timeframe(), report.barCount()),
                new StrategySection(request.strategyName(), request.shortEma(), request.longEma()),
                new PerformanceSection(
                        report.tradeCount(),
                        report.initialCash(),
                        report.finalValue(),
                        report.returnPct(),
                        report.winRatePct()),
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
            DatasetSection dataset,
            StrategySection strategy,
            PerformanceSection performance,
            List<String> notes) {
    }

    private record DatasetSection(
            String csvPath,
            String symbol,
            String timeframe,
            int bars) {
    }

    private record StrategySection(
            String name,
            int shortEma,
            int longEma) {
    }

    private record PerformanceSection(
            int closedTrades,
            double initialCash,
            double finalValue,
            double returnPct,
            double winRatePct) {
    }
}
