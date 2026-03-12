package ch.lueem.tradingbot.strategy.signal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Returns preconfigured signals from a deterministic queue for local bot runtime cycles.
 */
public class QueuedSignalEvaluator implements StrategySignalEvaluator {

    private final Deque<TradeSignal> signals;

    public QueuedSignalEvaluator(List<TradeSignal> signals) {
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
