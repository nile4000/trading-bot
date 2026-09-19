package ch.lueem.tradingbot.adapters.execution.binance.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Holds the Binance LOT_SIZE rules needed to create a sellable quantity. */
public record BinanceLotSize(BigDecimal minQty, BigDecimal stepSize) {

    public BinanceLotSize {
        if (minQty == null || minQty.signum() < 0) {
            throw new IllegalArgumentException("minQty must not be negative.");
        }
        if (stepSize == null || stepSize.signum() <= 0) {
            throw new IllegalArgumentException("stepSize must be greater than zero.");
        }
    }

    public BigDecimal sellableQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() < 0) {
            throw new IllegalArgumentException("quantity must not be negative.");
        }
        BigDecimal rounded = quantity.divide(stepSize, 0, RoundingMode.DOWN).multiply(stepSize);
        return rounded.compareTo(minQty) < 0 ? BigDecimal.ZERO : rounded;
    }
}
