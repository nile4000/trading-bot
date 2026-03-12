package ch.lueem.tradingbot.integration.binance.spottestnet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import ch.lueem.tradingbot.bot.market.MarketSnapshot;
import ch.lueem.tradingbot.bot.market.MarketSnapshotProvider;
import ch.lueem.tradingbot.runtime.TradingDefinition;

/**
 * Loads the latest symbol price from Binance Spot Testnet using the REST ticker endpoint.
 */
public class BinanceTickerPriceMarketSnapshotProvider implements MarketSnapshotProvider {

    private final BinanceSpotTestnetClient client;

    public BinanceTickerPriceMarketSnapshotProvider(BinanceSpotTestnetClient client) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null.");
        }
        this.client = client;
    }

    @Override
    public MarketSnapshot load(TradingDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null.");
        }

        BigDecimal price = client.loadSymbolPrice(definition.symbol());
        return new MarketSnapshot(
                definition.symbol(),
                definition.timeframe(),
                OffsetDateTime.now(ZoneOffset.UTC),
                price,
                List.of(price));
    }
}
