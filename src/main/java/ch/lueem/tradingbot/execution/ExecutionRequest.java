package ch.lueem.tradingbot.execution;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.strategy.signal.TradeSignal;

/**
 * Describes one execution request created by a bot cycle.
 */
public record ExecutionRequest(
        String botId,
        String symbol,
        String timeframe,
        TradeSignal tradeSignal,
        OffsetDateTime requestedAt,
        BigDecimal referencePrice
) {
}
