package ch.lueem.tradingbot.adapters.market;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

class CsvMarketSnapshotProviderTest {

    @Test
    void load_preservesDecimalPrecisionWithoutDoubleRoundTrip() {
        var series = new BaseBarSeriesBuilder().withName("precision-series").build();
        addBar(series, "2026-03-12T22:20:00Z", "0.12345678");
        addBar(series, "2026-03-12T22:21:00Z", "123456.78901234");

        var provider = new CsvMarketSnapshotProvider(series, "BTCUSDT", "1m");
        var definition = backtestDefinition();

        var first = provider.load(definition);
        var second = provider.load(definition);

        assertEquals("0.12345678", first.lastPrice().toPlainString());
        assertEquals("123456.78901234", second.lastPrice().toPlainString());
    }

    private TradingDefinition backtestDefinition() {
        return new TradingDefinition(
                "backtest-btcusdt-1m",
                "v1",
                BotMode.BACKTEST,
                "BTCUSDT",
                "1m",
                new StrategyDefinition("ema_cross", null));
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
