package ch.lueem.tradingbot.adapters.config;

import java.math.BigDecimal;

/**
 * Holds the explicit execution assumptions for one paper bot runtime.
 */
public record PaperExecutionConfig(
        PaperExchange exchange,
        PaperOrderMode orderMode,
        long tickIntervalMillis,
        double initialCash,
        BigDecimal orderQuantity
) {
    public void validate() {
        if (exchange == null) {
            throw new IllegalStateException("paper.execution.exchange must not be null.");
        }
        if (orderMode == null) {
            throw new IllegalStateException("paper.execution.orderMode must not be null.");
        }
        if (tickIntervalMillis <= 0L) {
            throw new IllegalStateException("paper.execution.tickIntervalMillis must be greater than zero.");
        }
        if (initialCash <= 0.0) {
            throw new IllegalStateException("paper.execution.initialCash must be greater than zero.");
        }
        if (orderQuantity == null || orderQuantity.signum() <= 0) {
            throw new IllegalStateException("paper.execution.orderQuantity must be greater than zero.");
        }
        if (orderMode != PaperOrderMode.VALIDATE_ONLY) {
            throw new IllegalStateException("paper.execution.orderMode=%s is not supported in phase 1."
                    .formatted(orderMode));
        }
    }
}
