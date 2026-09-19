package ch.lueem.tradingbot.adapters.market;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.AbstractList;
import java.util.List;
import java.util.Objects;

import ch.lueem.tradingbot.adapters.binance.client.BinanceMarketDataClient;
import ch.lueem.tradingbot.core.runtime.MarketSnapshot;
import ch.lueem.tradingbot.core.runtime.MarketSnapshotProvider;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

/** Loads completed Binance klines and exposes them as a ta4j market series. */
public class BinanceKlineSnapshotProvider implements MarketSnapshotProvider {

    /** Maximum number of klines Binance returns per request. */
    private static final int MAX_KLINE_LIMIT = 1000;
    /** The newest kline is still running and is filtered out as not completed. */
    private static final int RUNNING_BAR = 1;
    private static final int MAX_HISTORY_BARS = MAX_KLINE_LIMIT - RUNNING_BAR;
    /** Latest completed kline plus the running one. */
    private static final int POLL_LIMIT = 2;

    private final BinanceMarketDataClient client;
    private final Clock clock;
    private final BarSeries series;

    public BinanceKlineSnapshotProvider(BinanceMarketDataClient client) {
        this(client, Clock.systemUTC());
    }

    BinanceKlineSnapshotProvider(BinanceMarketDataClient client, Clock clock) {
        if (client == null || clock == null) {
            throw new IllegalArgumentException("client and clock must not be null.");
        }
        this.client = client;
        this.clock = clock;
        this.series = new BaseBarSeriesBuilder().withName("paper-binance-klines").build();
    }

    public synchronized void initialize(TradingDefinition definition, int historyBarCount) {
        Objects.requireNonNull(definition, "definition must not be null.");
        if (historyBarCount < 1 || historyBarCount > MAX_HISTORY_BARS) {
            throw new IllegalArgumentException(
                    "historyBarCount must be between 1 and %d.".formatted(MAX_HISTORY_BARS));
        }
        var completed = loadCompletedKlines(definition, historyBarCount + RUNNING_BAR);
        int first = Math.max(0, completed.size() - historyBarCount);
        completed.subList(first, completed.size()).forEach(this::appendIfNew);
        requireCompletedBars(definition);
    }

    @Override
    public synchronized MarketSnapshot load(TradingDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null.");
        loadCompletedKlines(definition, POLL_LIMIT).forEach(this::appendIfNew);
        requireCompletedBars(definition);

        int index = series.getEndIndex();
        var bar = series.getBar(index);
        return new MarketSnapshot(
                definition.symbol(),
                definition.timeframe(),
                bar.getEndTime().atOffset(ZoneOffset.UTC),
                decimal(bar.getClosePrice()),
                closePriceHistoryView(index),
                index);
    }

    public synchronized BarSeries series() {
        return series;
    }

    private List<BinanceKline> loadCompletedKlines(TradingDefinition definition, int limit) {
        var now = clock.instant();
        return client.loadKlines(definition.symbol(), definition.timeframe(), limit).stream()
                .filter(kline -> !kline.closeTime().toInstant().isAfter(now))
                .toList();
    }

    private void requireCompletedBars(TradingDefinition definition) {
        if (series.isEmpty()) {
            throw new IllegalStateException(
                    "Binance returned no completed klines for %s %s."
                            .formatted(definition.symbol(), definition.timeframe()));
        }
    }

    private void appendIfNew(BinanceKline kline) {
        boolean alreadyKnown = !series.isEmpty()
                && !kline.closeTime().toInstant().isAfter(series.getLastBar().getEndTime());
        if (!alreadyKnown) {
            series.addBar(toBar(kline));
        }
    }

    private org.ta4j.core.Bar toBar(BinanceKline kline) {
        return series.barBuilder()
                .timePeriod(java.time.Duration.between(kline.openTime(), kline.closeTime()).plusMillis(1))
                .endTime(kline.closeTime().toInstant())
                .openPrice(kline.open().toPlainString())
                .highPrice(kline.high().toPlainString())
                .lowPrice(kline.low().toPlainString())
                .closePrice(kline.close().toPlainString())
                .volume(kline.volume().toPlainString())
                .build();
    }

    private List<BigDecimal> closePriceHistoryView(int endIndexInclusive) {
        return new AbstractList<>() {
            @Override
            public BigDecimal get(int index) {
                if (index < 0 || index > endIndexInclusive) {
                    throw new IndexOutOfBoundsException("index: " + index + ", size: " + size());
                }
                return decimal(series.getBar(index).getClosePrice());
            }

            @Override
            public int size() {
                return endIndexInclusive + 1;
            }
        };
    }

    private BigDecimal decimal(org.ta4j.core.num.Num value) {
        return new BigDecimal(value.toString());
    }
}