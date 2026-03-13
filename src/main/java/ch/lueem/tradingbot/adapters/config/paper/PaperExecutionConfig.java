package ch.lueem.tradingbot.adapters.config.paper;

import java.math.BigDecimal;

/**
 * Holds the explicit execution assumptions for one paper bot runtime.
 */
public record PaperExecutionConfig(
        PaperExchange exchange,
        PaperOrderMode orderMode,
        long tickIntervalMillis,
        double initialCash,
        BigDecimal orderQuantity,
        boolean placeOrdersEnabled,
        BigDecimal maxOrderNotional
) {
    public PaperExecutionConfig {
        if (orderMode == PaperOrderMode.PLACE_ORDER) {
            if (!placeOrdersEnabled) {
                throw new IllegalStateException(
                        "paper.execution.placeOrdersEnabled must be true when paper.execution.orderMode=PLACE_ORDER.");
            }
            if (maxOrderNotional == null || maxOrderNotional.signum() <= 0) {
                throw new IllegalStateException(
                        "paper.execution.maxOrderNotional must be greater than zero when paper.execution.orderMode=PLACE_ORDER.");
            }
        }
    }
}
