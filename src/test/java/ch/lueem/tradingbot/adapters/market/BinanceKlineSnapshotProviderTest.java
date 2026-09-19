package ch.lueem.tradingbot.adapters.market;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;

import ch.lueem.tradingbot.adapters.binance.client.BinanceMarketDataClient;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceSymbolInfo;
import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import org.junit.jupiter.api.Test;

class BinanceKlineSnapshotProviderTest {

    @Test
    void initializesWithCompletedHistoryAndAppendsEachClosedKlineOnce() {
        var client = new KlineClient(
                List.of(kline("10:00", "100"), kline("10:01", "101"), kline("10:02", "102"), openKline()),
                List.of(kline("10:02", "102"), kline("10:03", "103")),
                List.of(kline("10:02", "102"), kline("10:03", "103")));
        var provider = new BinanceKlineSnapshotProvider(
                client, Clock.fixed(Instant.parse("2026-09-19T10:04:30Z"), ZoneOffset.UTC));
        var definition = definition();

        provider.initialize(definition, 3);
        var first = provider.load(definition);
        var repeated = provider.load(definition);

        assertEquals(4, provider.series().getBarCount());
        assertEquals(3, first.barIndex());
        assertEquals(new BigDecimal("103"), first.lastPrice());
        assertEquals(first.observedAt(), repeated.observedAt());
    }

    private BinanceKline kline(String minute, String close) {
        OffsetDateTime open = OffsetDateTime.parse("2026-09-19T" + minute + ":00Z");
        return new BinanceKline(open, open.plusMinutes(1).minusNanos(1_000_000),
                new BigDecimal(close), new BigDecimal(close), new BigDecimal(close),
                new BigDecimal(close), BigDecimal.ONE);
    }

    private BinanceKline openKline() {
        OffsetDateTime open = OffsetDateTime.parse("2026-09-19T10:04:00Z");
        return new BinanceKline(open, open.plusMinutes(1).minusNanos(1_000_000),
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
    }

    private TradingDefinition definition() {
        return new TradingDefinition(
                "bot-1", "v1", BotMode.PAPER, "BTCUSDT", "1m",
                new StrategyDefinition("ema_cross", null));
    }

    private static final class KlineClient implements BinanceMarketDataClient {
        private final ArrayDeque<List<BinanceKline>> responses;

        @SafeVarargs
        private KlineClient(List<BinanceKline>... responses) {
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        @Override
        public List<BinanceKline> loadKlines(String symbol, String timeframe, int limit) {
            return responses.removeFirst();
        }

        @Override
        public BinanceSymbolInfo loadSymbolInfo(String symbol) {
            throw new UnsupportedOperationException();
        }

    }
}
