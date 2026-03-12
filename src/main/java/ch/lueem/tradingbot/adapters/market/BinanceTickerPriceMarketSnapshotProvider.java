package ch.lueem.tradingbot.adapters.market;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClient;
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

    private final BinanceSpotTestnetClient client;
    private final BarSeries series;
    private final List<BigDecimal> closePriceHistory;
    private int nextBarIndex;

    public BinanceTickerPriceMarketSnapshotProvider(BinanceSpotTestnetClient client) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null.");
        }
        this.client = client;
        this.series = new BaseBarSeriesBuilder().withName("paper-live-series").build();
        this.closePriceHistory = new ArrayList<>();
        this.nextBarIndex = 0;
    }

    @Override
    public synchronized MarketSnapshot load(TradingDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null.");
        }

        BigDecimal price = client.loadSymbolPrice(definition.symbol());
        Duration timeframe = parseTimeframe(definition.timeframe());
        OffsetDateTime observedAt = nextObservedAt(timeframe);
        series.addBar(series.barBuilder()
                .timePeriod(timeframe)
                .endTime(observedAt.toInstant())
                .openPrice(price.toPlainString())
                .highPrice(price.toPlainString())
                .lowPrice(price.toPlainString())
                .closePrice(price.toPlainString())
                .volume("0")
                .build());
        closePriceHistory.add(price);
        return new MarketSnapshot(
                definition.symbol(),
                definition.timeframe(),
                observedAt,
                price,
                List.copyOf(closePriceHistory),
                nextBarIndex++);
    }

    public synchronized BarSeries series() {
        return series;
    }

    private Duration parseTimeframe(String timeframe) {
        return CsvHistoricalMarketSnapshotProvider.parseTimeframe(timeframe);
    }

    private OffsetDateTime nextObservedAt(Duration timeframe) {
        if (series.isEmpty()) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        return series.getLastBar().getEndTime().atOffset(ZoneOffset.UTC).plus(timeframe);
    }
}
