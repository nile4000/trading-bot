package ch.lueem.tradingbot.adapters.execution.binance.model;

/** Holds the Binance symbol metadata required by paper execution. */
public record BinanceSymbolInfo(
        String symbol,
        String baseAsset,
        String quoteAsset,
        BinanceLotSize lotSize) {
}
