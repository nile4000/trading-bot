package ch.lueem.tradingbot.portfolio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Describes the currently open position, if any.
 */
public record PositionSnapshot(
        boolean open,
        BigDecimal quantity,
        BigDecimal entryPrice,
        OffsetDateTime openedAt) {
            
    public static PositionSnapshot flat() {
        return new PositionSnapshot(false, null, null, null);
    }
}
