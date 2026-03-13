package ch.lueem.tradingbot.adapters.market;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.AbstractList;
import java.util.List;

import ch.lueem.tradingbot.core.time.Timeframes;
import ch.lueem.tradingbot.core.runtime.MarketSnapshot;
import ch.lueem.tradingbot.core.runtime.MarketSnapshotProvider;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import org.ta4j.core.BarSeries;

/**
 * Replays historical CSV bars as sequential market snapshots for the shared runtime.
 */
public class CsvMarketSnapshotProvider implements MarketSnapshotProvider {

    private final BarSeries series;
    private int nextIndex;

    public CsvMarketSnapshotProvider(
            CsvBarSeriesLoader csvBarSeriesLoader,
            Path csvPath,
            String symbol,
            String timeframe) {
        this(loadSeries(csvBarSeriesLoader, csvPath, symbol, timeframe), symbol, timeframe);
    }

    public CsvMarketSnapshotProvider(BarSeries series, String symbol, String timeframe) {
        if (series == null) {
            throw new IllegalArgumentException("series must not be null.");
        }
        if ((symbol == null || symbol.isBlank()) || (timeframe == null || timeframe.isBlank())) {
            throw new IllegalArgumentException("symbol and timeframe must not be blank.");
        }
        this.series = series;
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.nextIndex = 0;
    }

    private final String symbol;
    private final String timeframe;

    @Override
    public MarketSnapshot load(TradingDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null.");
        }
        if (nextIndex >= series.getBarCount()) {
            throw new IllegalStateException("No more historical market snapshots available for runtime " + definition.runtimeId());
        }
        int barIndex = nextIndex++;
        var closePrice = toBigDecimal(series.getBar(barIndex).getClosePrice());
        return new MarketSnapshot(
                symbol,
                timeframe,
                series.getBar(barIndex).getEndTime().atOffset(ZoneOffset.UTC),
                closePrice,
                closePriceHistoryView(barIndex),
                barIndex);
    }

    public int snapshotCount() {
        return series.getBarCount();
    }

    public BarSeries series() {
        return series;
    }

    private List<BigDecimal> closePriceHistoryView(int endIndexInclusive) {
        if (series.isEmpty()) {
            throw new IllegalArgumentException("Historical series must not be empty.");
        }
        return new AbstractList<>() {
            @Override
            public BigDecimal get(int index) {
                if (index < 0 || index > endIndexInclusive) {
                    throw new IndexOutOfBoundsException("index: " + index + ", size: " + size());
                }
                return toBigDecimal(series.getBar(index).getClosePrice());
            }

            @Override
            public int size() {
                return endIndexInclusive + 1;
            }
        };
    }

    private static BarSeries loadSeries(
            CsvBarSeriesLoader csvBarSeriesLoader,
            Path csvPath,
            String symbol,
            String timeframe) {
        if (csvBarSeriesLoader == null || csvPath == null) {
            throw new IllegalArgumentException("csvBarSeriesLoader and csvPath must not be null.");
        }
        if ((symbol == null || symbol.isBlank()) || (timeframe == null || timeframe.isBlank())) {
            throw new IllegalArgumentException("symbol and timeframe must not be blank.");
        }
        return csvBarSeriesLoader.load(csvPath, symbol + "-" + timeframe, Timeframes.parse(timeframe));
    }

    private static BigDecimal toBigDecimal(org.ta4j.core.num.Num value) {
        return new BigDecimal(value.toString());
    }
}
