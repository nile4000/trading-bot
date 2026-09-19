package ch.lueem.tradingbot.adapters.binance.client;

import java.util.List;

import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceSymbolInfo;
import ch.lueem.tradingbot.adapters.market.BinanceKline;

/** Loads Binance symbol metadata and market data. */
public interface BinanceMarketDataClient {

    BinanceSymbolInfo loadSymbolInfo(String symbol);

    List<BinanceKline> loadKlines(String symbol, String timeframe, int limit);
}
