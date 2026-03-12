package ch.lueem.tradingbot.core.strategy.action;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Returns preconfigured actions from a deterministic queue for local bot runtime cycles.
 */
public class QueuedActionEvaluator implements StrategyActionEvaluator {

    private final Deque<TradeAction> actions;

    public QueuedActionEvaluator(List<TradeAction> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("actions must not be null or empty.");
        }
        this.actions = new ArrayDeque<>(actions);
    }

    @Override
    public TradeAction evaluate(ActionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null.");
        }

        return actions.isEmpty() ? TradeAction.HOLD : actions.removeFirst();
    }
}
