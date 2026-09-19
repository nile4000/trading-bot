package ch.lueem.tradingbot.adapters.market;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Represents one completed Binance candlestick. */
public record BinanceKline(
        OffsetDateTime openTime,
        OffsetDateTime closeTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume) {
}
