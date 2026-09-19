package ch.lueem.tradingbot.adapters.execution.binance.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.core.strategy.action.TradeAction;

/** Holds the Binance order fields needed for recovery and reconciliation. */
public record BinanceOrder(
        long orderId,
        String clientOrderId,
        String symbol,
        TradeAction action,
        String status,
        BigDecimal requestedQuantity,
        BigDecimal executedQuantity,
        BigDecimal cumulativeQuoteQuantity,
        OffsetDateTime updatedAt) {
}
