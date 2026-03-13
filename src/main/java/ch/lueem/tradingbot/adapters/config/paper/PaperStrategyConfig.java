package ch.lueem.tradingbot.adapters.config.paper;

import java.util.List;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;

/**
 * Holds the configured strategy for one paper bot runtime.
 */
public record PaperStrategyConfig(
        String name,
        StrategyParameters parameters,
        List<TradeAction> actions) {

    private static final String QUEUED_ACTIONS = "queued_actions";

    public PaperStrategyConfig {
        if (QUEUED_ACTIONS.equals(name)) {
            if (actions == null || actions.isEmpty()) {
                throw new IllegalStateException("paper.strategy.actions must not be null or empty for queued_actions.");
            }
        } else if (parameters == null) {
            throw new IllegalStateException("paper.strategy.parameters must not be null for strategy " + name + ".");
        }
    }

    public StrategyDefinition toStrategyDefinition() {
        return new StrategyDefinition(name, parameters);
    }
}
