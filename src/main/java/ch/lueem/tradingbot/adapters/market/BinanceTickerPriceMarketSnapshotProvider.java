package ch.lueem.tradingbot.adapters.market;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClient;
import ch.lueem.tradingbot.core.time.Timeframes;
import ch.lueem.tradingbot.core.runtime.MarketSnapshot;
import ch.lueem.tradingbot.core.runtime.MarketSnapshotProvider;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

/**
 * Loads the latest symbol price from Binance Spot Testnet and keeps a simple
 * close-price history for strategy evaluation.
 */
public class BinanceTickerPriceMarketSnapshotProvider implements MarketSnapshotProvider {

    private final BinanceClient client;
    private final Clock clock;
    private final BarSeries series;
    private final List<BigDecimal> closePriceHistory;

    public BinanceTickerPriceMarketSnapshotProvider(BinanceClient client) {
        this(client, Clock.systemUTC());
    }

    BinanceTickerPriceMarketSnapshotProvider(BinanceClient client, Clock clock) {
        if (client == null || clock == null) {
            throw new IllegalArgumentException("client and clock must not be null.");
        }
        this.client = client;
        this.clock = clock;
        this.series = new BaseBarSeriesBuilder().withName("paper-live-series").build();
        this.closePriceHistory = new ArrayList<>();
    }

    @Override
    public synchronized MarketSnapshot load(TradingDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null.");
        }

        var price = client.loadSymbolPrice(definition.symbol());
        var timeframe = Timeframes.parse(definition.timeframe());
        var observedAt = clock.instant();
        var barEndTime = alignToBarEnd(observedAt, timeframe);

        upsertBar(timeframe, barEndTime, price);
        return new MarketSnapshot(
                definition.symbol(),
                definition.timeframe(),
                observedAt.atOffset(ZoneOffset.UTC),
                price,
                List.copyOf(closePriceHistory),
                series.getEndIndex());
    }

    public synchronized BarSeries series() {
        return series;
    }

    private void upsertBar(Duration timeframe, Instant barEndTime, BigDecimal price) {
        if (series.isEmpty()) {
            addBar(timeframe, barEndTime, price);
            return;
        }

        var lastBarEndTime = series.getLastBar().getEndTime();
        if (barEndTime.isBefore(lastBarEndTime)) {
            throw new IllegalStateException("Observed market time moved backwards for the live paper series.");
        }
        if (barEndTime.equals(lastBarEndTime)) {
            series.addPrice(price);
            closePriceHistory.set(closePriceHistory.size() - 1, price);
            return;
        }

        addBar(timeframe, barEndTime, price);
    }

    private void addBar(Duration timeframe, Instant barEndTime, BigDecimal price) {
        series.addBar(series.barBuilder()
                .timePeriod(timeframe)
                .endTime(barEndTime)
                .openPrice(price.toPlainString())
                .highPrice(price.toPlainString())
                .lowPrice(price.toPlainString())
                .closePrice(price.toPlainString())
                .volume("0")
                .build());
        closePriceHistory.add(price);
    }

    private Instant alignToBarEnd(Instant observedAt, Duration timeframe) {
        long timeframeSeconds = timeframe.toSeconds();
        long observedSeconds = observedAt.getEpochSecond();
        long nextBoundarySeconds = ((observedSeconds / timeframeSeconds) + 1) * timeframeSeconds;
        return Instant.ofEpochSecond(nextBoundarySeconds);
    }
}
