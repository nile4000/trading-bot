package ch.lueem.tradingbot.adapters.market;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Queue;

import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import org.junit.jupiter.api.Test;

class BinanceTickerPriceMarketSnapshotProviderTest {

    @Test
    void load_updatesCurrentBarWithinSameTimeframe() {
        var provider = new BinanceTickerPriceMarketSnapshotProvider(
                new StubClient("100.00", "101.00", "102.00"),
                new SequenceClock(
                        "2026-03-12T22:19:41Z",
                        "2026-03-12T22:19:52Z",
                        "2026-03-12T22:19:59Z"));
        var definition = btcDefinition();

        var first = provider.load(definition);
        var second = provider.load(definition);
        var third = provider.load(definition);
        var lastBar = provider.series().getLastBar();

        assertEquals(0, first.barIndex());
        assertEquals(0, second.barIndex());
        assertEquals(0, third.barIndex());
        assertEquals(1, first.closePriceHistory().size());
        assertEquals(1, second.closePriceHistory().size());
        assertEquals(1, third.closePriceHistory().size());
        assertEquals(new BigDecimal("102.00"), third.closePriceHistory().getFirst());
        assertEquals(1, provider.series().getBarCount());
        assertEquals(102.0, lastBar.getClosePrice().doubleValue());
        assertEquals(102.0, lastBar.getHighPrice().doubleValue());
        assertEquals(100.0, lastBar.getLowPrice().doubleValue());
    }

    @Test
    void load_appendsNewBarWhenTimeframeBoundaryIsCrossed() {
        var provider = new BinanceTickerPriceMarketSnapshotProvider(
                new StubClient("100.00", "101.00", "102.00"),
                new SequenceClock(
                        "2026-03-12T22:19:41Z",
                        "2026-03-12T22:20:02Z",
                        "2026-03-12T22:21:03Z"));
        var definition = btcDefinition();

        var first = provider.load(definition);
        var second = provider.load(definition);
        var third = provider.load(definition);

        assertEquals(0, first.barIndex());
        assertEquals(1, second.barIndex());
        assertEquals(2, third.barIndex());
        assertEquals(1, first.closePriceHistory().size());
        assertEquals(2, second.closePriceHistory().size());
        assertEquals(3, third.closePriceHistory().size());
        assertEquals(3, provider.series().getBarCount());
    }

    private TradingDefinition btcDefinition() {
        return new TradingDefinition(
                "paper-bot",
                "v1",
                BotMode.PAPER,
                "BTCUSDT",
                "1m",
                new StrategyDefinition("ema_cross", null));
    }

    private static final class StubClient implements ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClient {
        private final Queue<BigDecimal> prices;

        private StubClient(String... prices) {
            this.prices = new ArrayDeque<>();
            for (String price : prices) {
                this.prices.add(new BigDecimal(price));
            }
        }

        @Override
        public String baseUrl() {
            return "https://testnet.binance.vision";
        }

        @Override
        public BigDecimal loadSymbolPrice(String symbol) {
            return prices.remove();
        }

        @Override
        public void validateOrder(com.binance.connector.client.spot.rest.model.OrderTestRequest request) {
        }

        @Override
        public com.binance.connector.client.spot.rest.model.NewOrderResponse placeOrder(
                com.binance.connector.client.spot.rest.model.NewOrderRequest request) {
            throw new UnsupportedOperationException("placeOrder is not used in this test.");
        }
    }

    private static final class SequenceClock extends Clock {
        private final Queue<Instant> instants;

        private SequenceClock(String... instants) {
            this.instants = new ArrayDeque<>();
            for (String instant : instants) {
                this.instants.add(Instant.parse(instant));
            }
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instants.remove();
        }
    }
}
