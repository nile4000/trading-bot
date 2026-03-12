package ch.lueem.tradingbot.application;

import java.util.List;

import ch.lueem.tradingbot.strategy.signal.TradeSignal;

/**
 * Holds the configured signal source for one paper bot runtime.
 */
public record PaperSignalSourceConfig(
        String strategyName,
        List<TradeSignal> signals
) {
    public void validate() {
        if (strategyName == null || strategyName.isBlank()) {
            throw new IllegalStateException("paper.signalSource.strategyName must not be blank.");
        }
        if (signals == null || signals.isEmpty()) {
            throw new IllegalStateException("paper.signalSource.signals must not be null or empty.");
        }
    }
}
