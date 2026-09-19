package ch.lueem.tradingbot.adapters.execution.binance.model;

import java.math.BigDecimal;

/** Holds the available and locked balance for one Binance asset. */
public record BinanceBalance(String asset, BigDecimal free, BigDecimal locked) {

    public BigDecimal total() {
        return free.add(locked);
    }
}
