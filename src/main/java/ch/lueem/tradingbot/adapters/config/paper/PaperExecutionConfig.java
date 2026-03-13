package ch.lueem.tradingbot.adapters.config.paper;

import static ch.lueem.tradingbot.adapters.config.ConfigValidation.requirePositive;
import static ch.lueem.tradingbot.adapters.config.ConfigValidation.requirePresent;

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
        requirePresent(exchange, "paper.execution.exchange must not be null.");
        requirePresent(orderMode, "paper.execution.orderMode must not be null.");
        requirePositive(tickIntervalMillis, "paper.execution.tickIntervalMillis must be greater than zero.");
        requirePositive(initialCash, "paper.execution.initialCash must be greater than zero.");
        requirePositive(orderQuantity, "paper.execution.orderQuantity must be greater than zero.");

        if (orderMode == PaperOrderMode.PLACE_ORDER) {
            if (!placeOrdersEnabled) {
                throw new IllegalStateException(
                        "paper.execution.placeOrdersEnabled must be true when paper.execution.orderMode=PLACE_ORDER.");
            }
            requirePositive(
                    maxOrderNotional,
                    "paper.execution.maxOrderNotional must be greater than zero when paper.execution.orderMode=PLACE_ORDER.");
        }
    }
}
