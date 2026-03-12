package ch.lueem.tradingbot.adapters.market;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.core.runtime.MarketSnapshot;
import ch.lueem.tradingbot.core.runtime.MarketSnapshotProvider;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import org.ta4j.core.BarSeries;

/**
 * Replays historical CSV bars as sequential market snapshots for the shared runtime.
 */
public class CsvHistoricalMarketSnapshotProvider implements MarketSnapshotProvider {

    private final BarSeries series;
    private final List<MarketSnapshot> snapshots;
    private int nextIndex;

    public CsvHistoricalMarketSnapshotProvider(
            CsvBarSeriesLoader csvBarSeriesLoader,
            Path csvPath,
            String symbol,
            String timeframe) {
        this(loadSeries(csvBarSeriesLoader, csvPath, symbol, timeframe), symbol, timeframe);
    }

    public CsvHistoricalMarketSnapshotProvider(BarSeries series, String symbol, String timeframe) {
        if (series == null) {
            throw new IllegalArgumentException("series must not be null.");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank.");
        }
        if (timeframe == null || timeframe.isBlank()) {
            throw new IllegalArgumentException("timeframe must not be blank.");
        }
        this.series = series;
        this.snapshots = buildSnapshots(series, symbol, timeframe);
        this.nextIndex = 0;
    }

    @Override
    public MarketSnapshot load(TradingDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null.");
        }
        if (nextIndex >= snapshots.size()) {
            throw new IllegalStateException("No more historical market snapshots available for runtime " + definition.runtimeId());
        }
        return snapshots.get(nextIndex++);
    }

    public int snapshotCount() {
        return snapshots.size();
    }

    public BarSeries series() {
        return series;
    }

    private List<MarketSnapshot> buildSnapshots(BarSeries series, String symbol, String timeframe) {
        if (series.isEmpty()) {
            throw new IllegalArgumentException("Historical series must not be empty.");
        }
        List<MarketSnapshot> builtSnapshots = new ArrayList<>(series.getBarCount());
        List<BigDecimal> closeHistory = new ArrayList<>(series.getBarCount());
        for (int index = 0; index < series.getBarCount(); index++) {
            BigDecimal closePrice = BigDecimal.valueOf(series.getBar(index).getClosePrice().doubleValue());
            closeHistory.add(closePrice);
            builtSnapshots.add(new MarketSnapshot(
                    symbol,
                    timeframe,
                    series.getBar(index).getEndTime().atOffset(ZoneOffset.UTC),
                    closePrice,
                    List.copyOf(closeHistory),
                    index));
        }
        return List.copyOf(builtSnapshots);
    }

    public static Duration parseTimeframe(String timeframe) {
        return switch (timeframe) {
            case "1m" -> Duration.ofMinutes(1);
            case "5m" -> Duration.ofMinutes(5);
            case "15m" -> Duration.ofMinutes(15);
            case "1h" -> Duration.ofHours(1);
            case "4h" -> Duration.ofHours(4);
            case "1d" -> Duration.ofDays(1);
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        };
    }

    private static BarSeries loadSeries(
            CsvBarSeriesLoader csvBarSeriesLoader,
            Path csvPath,
            String symbol,
            String timeframe) {
        if (csvBarSeriesLoader == null) {
            throw new IllegalArgumentException("csvBarSeriesLoader must not be null.");
        }
        if (csvPath == null) {
            throw new IllegalArgumentException("csvPath must not be null.");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank.");
        }
        if (timeframe == null || timeframe.isBlank()) {
            throw new IllegalArgumentException("timeframe must not be blank.");
        }
        return csvBarSeriesLoader.load(csvPath, symbol + "-" + timeframe, parseTimeframe(timeframe));
    }
}
