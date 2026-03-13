package ch.lueem.tradingbot.adapters.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

class CsvHistoricalMarketSnapshotProviderTest {

    @Test
    void load_buildsSnapshotsLazilyFromSeries() {
        BarSeries series = new BaseBarSeriesBuilder().withName("test-series").build();
        addBar(series, "2026-03-12T22:20:00Z", "100");
        addBar(series, "2026-03-12T22:21:00Z", "101");
        addBar(series, "2026-03-12T22:22:00Z", "102");

        CsvHistoricalMarketSnapshotProvider provider = new CsvHistoricalMarketSnapshotProvider(series, "BTCUSDT", "1m");
        TradingDefinition definition = new TradingDefinition(
                "backtest-btcusdt-1m",
                "v1",
                BotMode.BACKTEST,
                "BTCUSDT",
                "1m",
                new StrategyDefinition("ema_cross", null));

        var first = provider.load(definition);
        var second = provider.load(definition);
        var third = provider.load(definition);

        assertEquals(3, provider.snapshotCount());
        assertEquals(0, first.barIndex());
        assertEquals(1, second.barIndex());
        assertEquals(2, third.barIndex());
        assertEquals(1, first.closePriceHistory().size());
        assertEquals(2, second.closePriceHistory().size());
        assertEquals(3, third.closePriceHistory().size());
        assertEquals("100", first.closePriceHistory().getFirst().toString());
        assertEquals("102", third.closePriceHistory().getLast().toString());
        assertThrows(IllegalStateException.class, () -> provider.load(definition));
    }

    @Test
    void load_preservesDecimalPrecisionWithoutDoubleRoundTrip() {
        BarSeries series = new BaseBarSeriesBuilder().withName("precision-series").build();
        addBar(series, "2026-03-12T22:20:00Z", "0.12345678");
        addBar(series, "2026-03-12T22:21:00Z", "123456.78901234");

        CsvHistoricalMarketSnapshotProvider provider = new CsvHistoricalMarketSnapshotProvider(series, "BTCUSDT", "1m");
        TradingDefinition definition = new TradingDefinition(
                "backtest-btcusdt-1m",
                "v1",
                BotMode.BACKTEST,
                "BTCUSDT",
                "1m",
                new StrategyDefinition("ema_cross", null));

        var first = provider.load(definition);
        var second = provider.load(definition);

        assertEquals("0.12345678", first.lastPrice().toPlainString());
        assertEquals("0.12345678", first.closePriceHistory().getFirst().toPlainString());
        assertEquals("123456.78901234", second.lastPrice().toPlainString());
        assertEquals("123456.78901234", second.closePriceHistory().getLast().toPlainString());
    }

    private void addBar(BarSeries series, String endTime, String closePrice) {
        series.addBar(series.barBuilder()
                .timePeriod(Duration.ofMinutes(1))
                .endTime(java.time.Instant.parse(endTime))
                .openPrice(closePrice)
                .highPrice(closePrice)
                .lowPrice(closePrice)
                .closePrice(closePrice)
                .volume("0")
                .build());
    }
}
