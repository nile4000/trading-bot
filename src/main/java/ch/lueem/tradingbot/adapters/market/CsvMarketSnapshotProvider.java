package ch.lueem.tradingbot.adapters.market;

import java.time.ZoneOffset;

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
        var bar = series.getBar(barIndex);
        return new MarketSnapshot(
                symbol,
                timeframe,
                bar.getEndTime().atOffset(ZoneOffset.UTC),
                bar.getClosePrice().bigDecimalValue(),
                bar.getOpenPrice().bigDecimalValue(),
                barIndex);
    }

    public int snapshotCount() {
        return series.getBarCount();
    }

}
