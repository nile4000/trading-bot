package ch.lueem.tradingbot.adapters.reporting;

import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.ReportingConfig;
import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.adapters.reporting.model.BacktestReportDocument;
import ch.lueem.tradingbot.modes.backtest.model.Report;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Converts a backtest result into a stable JSON document and writes it to an
 * output stream.
 */
public class BacktestReportJsonPrinter {

    private static final String REPORT_VERSION = "v5";
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
            Report report) {
        if (out == null || reporting == null || config == null || report == null) {
            throw new IllegalArgumentException("out, reporting, config, and report must not be null.");
        }

        out.println(toJson(toDocument(config, report, reporting), reporting));
    }

    private List<String> buildNotes(Report report, ReportingConfig reporting) {
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
        notes.add("Fees and slippage are not modeled in this report.");
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
            Report report,
            ReportingConfig reporting) {
        return new BacktestReportDocument(
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                REPORT_VERSION,
                buildMetadata(config, report),
                buildStrategy(report),
                buildPerformance(report),
                buildNotes(report, reporting));
    }

    private ObjectMapper selectObjectMapper(boolean prettyPrint) {
        return prettyPrint ? prettyObjectMapper : compactObjectMapper;
    }

    private BacktestReportDocument.Metadata buildMetadata(BacktestConfig config, Report report) {
        return new BacktestReportDocument.Metadata(
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

    private BacktestReportDocument.Strategy buildStrategy(Report report) {
        return new BacktestReportDocument.Strategy(
                report.metadata().strategy().name(),
                report.metadata().strategy().parameters());
    }

    private BacktestReportDocument.Performance buildPerformance(Report report) {
        return new BacktestReportDocument.Performance(
                report.closedTradeCount(),
                report.initialCash(),
                report.finalValue(),
                report.totalReturnPercent(),
                report.buyAndHoldReturnPercent(),
                report.maxDrawdownPercent(),
                report.profitFactor(),
                report.winRatePercent(),
                report.averageWinningTrade(),
                report.averageLosingTrade(),
                report.timeInMarketDays(),
                report.exposurePercent());
    }
}
