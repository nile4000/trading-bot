package ch.lueem.tradingbot.adapters.market;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Queue;

import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.runtime.MarketSnapshot;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import org.junit.jupiter.api.Test;

class BinanceTickerPriceMarketSnapshotProviderTest {

    @Test
    void load_buildsGrowingPriceHistoryAndBarIndex() {
        BinanceTickerPriceMarketSnapshotProvider provider = new BinanceTickerPriceMarketSnapshotProvider(
                new StubClient("100.00", "101.00", "102.00"));
        TradingDefinition definition = new TradingDefinition(
                "paper-bot",
                "v1",
                BotMode.PAPER,
                "BTCUSDT",
                "1m",
                new StrategyDefinition("ema_cross", null));

        MarketSnapshot first = provider.load(definition);
        MarketSnapshot second = provider.load(definition);
        MarketSnapshot third = provider.load(definition);

        assertEquals(0, first.barIndex());
        assertEquals(1, second.barIndex());
        assertEquals(2, third.barIndex());
        assertEquals(1, first.closePriceHistory().size());
        assertEquals(2, second.closePriceHistory().size());
        assertEquals(3, third.closePriceHistory().size());
        assertEquals(3, provider.series().getBarCount());
    }

    private static final class StubClient implements ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClient {
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
    }
}
