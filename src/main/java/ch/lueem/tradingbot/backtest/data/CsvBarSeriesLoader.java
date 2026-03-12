package ch.lueem.tradingbot.backtest.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

/**
 * Loads OHLCV bars from a simple CSV file into a ta4j BarSeries.
 */
public class CsvBarSeriesLoader {

    private static final String EXPECTED_HEADER = "timestamp,open,high,low,close,volume";

    public BarSeries load(Path csvPath, String seriesName, Duration barDuration) {
        validateInputs(csvPath, seriesName, barDuration);

        List<String> lines = readLines(csvPath);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty: " + csvPath);
        }

        String header = lines.get(0).trim();
        if (!EXPECTED_HEADER.equals(header)) {
            throw new IllegalArgumentException(
                    "Invalid CSV header in %s. Expected '%s' but got '%s'."
                            .formatted(csvPath, EXPECTED_HEADER, header));
        }

        BarSeries series = new BaseBarSeriesBuilder().withName(seriesName).build();
        for (int lineNumber = 2; lineNumber <= lines.size(); lineNumber++) {
            String line = lines.get(lineNumber - 1).trim();
            if (line.isEmpty()) {
                continue;
            }
            addBar(series, line, lineNumber, barDuration, csvPath);
        }

        return series;
    }

    private void validateInputs(Path csvPath, String seriesName, Duration barDuration) {
        if (csvPath == null) {
            throw new IllegalArgumentException("csvPath must not be null.");
        }
        if (!Files.isRegularFile(csvPath)) {
            throw new IllegalArgumentException("CSV file does not exist: " + csvPath);
        }
        if (seriesName == null || seriesName.isBlank()) {
            throw new IllegalArgumentException("seriesName must not be blank.");
        }
        if (barDuration == null || barDuration.isZero() || barDuration.isNegative()) {
            throw new IllegalArgumentException("barDuration must be greater than zero.");
        }
    }

    private List<String> readLines(Path csvPath) {
        try {
            return Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read CSV file: " + csvPath, exception);
        }
    }

    private void addBar(BarSeries series, String line, int lineNumber, Duration barDuration, Path csvPath) {
        String[] columns = line.split(",", -1);
        if (columns.length != 6) {
            throw invalidLine(csvPath, lineNumber, "Expected 6 columns but got " + columns.length + ".");
        }

        try {
            Instant endTime = Instant.parse(columns[0].trim());
            String open = columns[1].trim();
            String high = columns[2].trim();
            String low = columns[3].trim();
            String close = columns[4].trim();
            String volume = columns[5].trim();

            ensureNumeric(open, "open");
            ensureNumeric(high, "high");
            ensureNumeric(low, "low");
            ensureNumeric(close, "close");
            ensureNumeric(volume, "volume");

            series.addBar(series.barBuilder()
                    .timePeriod(barDuration)
                    .endTime(endTime)
                    .openPrice(open)
                    .highPrice(high)
                    .lowPrice(low)
                    .closePrice(close)
                    .volume(volume)
                    .build());
        } catch (RuntimeException exception) {
            throw invalidLine(csvPath, lineNumber, exception.getMessage(), exception);
        }
    }

    private void ensureNumeric(String value, String columnName) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing value for column '%s'.".formatted(columnName));
        }
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid numeric value '%s' for column '%s'.".formatted(value, columnName),
                    exception);
        }
    }

    private IllegalArgumentException invalidLine(Path csvPath, int lineNumber, String message) {
        return invalidLine(csvPath, lineNumber, message, null);
    }

    private IllegalArgumentException invalidLine(Path csvPath, int lineNumber, String message, Exception cause) {
        return new IllegalArgumentException(
                "Invalid CSV row %d in %s: %s".formatted(lineNumber, csvPath, message),
                cause);
    }
}
