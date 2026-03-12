package ch.lueem.tradingbot.portfolio;

import java.math.BigDecimal;

/**
 * Holds the bot-facing portfolio view for one symbol at one point in time.
 */
public record PortfolioSnapshot(
        String symbol,
        BigDecimal availableCash,
        PositionSnapshot position
) {
}
