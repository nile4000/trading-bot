package ch.lueem.tradingbot.integration.backtest;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.backtest.data.CsvBarSeriesLoader;
import ch.lueem.tradingbot.bot.market.MarketSnapshot;
import ch.lueem.tradingbot.bot.market.MarketSnapshotProvider;
import ch.lueem.tradingbot.runtime.TradingDefinition;
import org.ta4j.core.BarSeries;

/**
 * Replays historical CSV bars as sequential market snapshots for the shared runtime.
 */
public class CsvHistoricalMarketSnapshotProvider implements MarketSnapshotProvider {

    private final List<MarketSnapshot> snapshots;
    private int nextIndex;

    public CsvHistoricalMarketSnapshotProvider(
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
        this.snapshots = buildSnapshots(
                csvBarSeriesLoader.load(csvPath, symbol + "-" + timeframe, parseTimeframe(timeframe)),
                symbol,
                timeframe);
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
                    List.copyOf(closeHistory)));
        }
        return List.copyOf(builtSnapshots);
    }

    private Duration parseTimeframe(String timeframe) {
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
}
