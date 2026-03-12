package ch.lueem.tradingbot.core.execution;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.core.strategy.action.TradeAction;

/**
 * Describes one execution request created by a runtime cycle.
 */
public record ExecutionRequest(
        String runtimeId,
        String symbol,
        String timeframe,
        TradeAction tradeAction,
        OffsetDateTime requestedAt,
        BigDecimal referencePrice
) {
}
