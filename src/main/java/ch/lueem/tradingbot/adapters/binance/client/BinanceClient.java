package ch.lueem.tradingbot.adapters.binance.client;

/** Complete Binance client created for a paper-bot session. */
public interface BinanceClient extends BinanceMarketDataClient, BinanceAccountClient, BinanceOrderClient {

    String baseUrl();
}
