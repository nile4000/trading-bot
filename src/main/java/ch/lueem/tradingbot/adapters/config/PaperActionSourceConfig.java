package ch.lueem.tradingbot.adapters.config;

import java.util.List;

import ch.lueem.tradingbot.core.strategy.action.TradeAction;

/**
 * Holds the configured action source for one paper bot runtime.
 */
public record PaperActionSourceConfig(
        String strategyName,
        List<TradeAction> actions
) {
    private static final String SUPPORTED_STRATEGY_NAME = "queued_actions";

    public void validate() {
        if (strategyName == null || strategyName.isBlank()) {
            throw new IllegalStateException("paper.actionSource.strategyName must not be blank.");
        }
        if (!SUPPORTED_STRATEGY_NAME.equals(strategyName)) {
            throw new IllegalStateException("paper.actionSource.strategyName=%s is not supported in phase 1."
                    .formatted(strategyName));
        }
        if (actions == null || actions.isEmpty()) {
            throw new IllegalStateException("paper.actionSource.actions must not be null or empty.");
        }
    }
}
