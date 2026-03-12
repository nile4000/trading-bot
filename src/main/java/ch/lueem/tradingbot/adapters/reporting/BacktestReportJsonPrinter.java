package ch.lueem.tradingbot.adapters.reporting;

import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.BacktestConfig;
import ch.lueem.tradingbot.adapters.config.ReportingConfig;
import ch.lueem.tradingbot.modes.backtest.model.BacktestReport;
import ch.lueem.tradingbot.adapters.reporting.model.BacktestMetadataSection;
import ch.lueem.tradingbot.adapters.reporting.model.BacktestPerformanceSection;
import ch.lueem.tradingbot.adapters.reporting.model.BacktestReportDocument;
import ch.lueem.tradingbot.adapters.reporting.model.BacktestStrategySection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Converts a backtest result into a stable JSON document and writes it to an
 * output stream.
 */
public class BacktestReportJsonPrinter {

    private static final String REPORT_VERSION = "v4";
    private final ObjectMapper prettyObjectMapper;
    private final ObjectMapper compactObjectMapper;

    public BacktestReportJsonPrinter() {
        this.prettyObjectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.compactObjectMapper = new ObjectMapper();
    }

    public void print(
            PrintStream out,
            ReportingConfig reporting,
            BacktestConfig config,
            BacktestReport report) {
        if (out == null) {
            throw new IllegalArgumentException("out must not be null.");
        }
        if (reporting == null) {
            throw new IllegalArgumentException("reporting must not be null.");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null.");
        }
        if (report == null) {
            throw new IllegalArgumentException("report must not be null.");
        }

        out.println(toJson(toDocument(config, report, reporting), reporting));
    }

    private List<String> buildNotes(BacktestReport report, ReportingConfig reporting) {
        List<String> notes = new ArrayList<>();
        if (!reporting.includeNotes()) {
            return notes;
        }
        if (report.closedTradeCount() == 0) {
            notes.add("No closed trade was generated on this dataset.");
        }
        if (report.hasOpenPosition()) {
            notes.add("An open position remains at the end of the backtest and is valued mark-to-market.");
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

    private BacktestReportDocument toDocument(
            BacktestConfig config,
            BacktestReport report,
            ReportingConfig reporting) {
        return new BacktestReportDocument(
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                REPORT_VERSION,
                buildMetadataSection(config, report),
                buildStrategySection(report),
                buildPerformanceSection(report),
                report.positions(),
                buildNotes(report, reporting));
    }

    private ObjectMapper selectObjectMapper(boolean prettyPrint) {
        return prettyPrint ? prettyObjectMapper : compactObjectMapper;
    }

    private BacktestMetadataSection buildMetadataSection(BacktestConfig config, BacktestReport report) {
        return new BacktestMetadataSection(
                report.metadata().mode(),
                config.csvPath().toString(),
                report.metadata().symbol(),
                report.metadata().timeframe(),
                report.metadata().barCount(),
                report.metadata().dataStart(),
                report.metadata().dataEnd(),
                report.metadata().executionModel(),
                report.metadata().positionSizingModel());
    }

    private BacktestStrategySection buildStrategySection(BacktestReport report) {
        return new BacktestStrategySection(
                report.metadata().strategy().name(),
                report.metadata().strategy().parameters());
    }

    private BacktestPerformanceSection buildPerformanceSection(BacktestReport report) {
        return new BacktestPerformanceSection(
                report.executedSignalCount(),
                report.closedTradeCount(),
                report.hasOpenPosition(),
                report.initialCash(),
                report.finalValue(),
                report.totalReturnPercent(),
                report.winRatePercent());
    }
}
