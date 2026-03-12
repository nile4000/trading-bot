package ch.lueem.tradingbot.strategy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Returns preconfigured signals from an in-memory queue for deterministic bot runtime cycles.
 */
public class InMemorySignalQueueEvaluator implements StrategySignalEvaluator {

    private final Deque<TradeSignal> signals;

    public InMemorySignalQueueEvaluator(List<TradeSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            throw new IllegalArgumentException("signals must not be null or empty.");
        }
        this.signals = new ArrayDeque<>(signals);
    }

    @Override
    public TradeSignal evaluate(SignalContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null.");
        }

        return signals.isEmpty() ? TradeSignal.HOLD : signals.removeFirst();
    }
}
