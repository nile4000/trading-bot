package ch.lueem.tradingbot.bot.market;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import ch.lueem.tradingbot.runtime.TradingDefinition;

/**
 * Provides a deterministic sequence of market snapshots for local bot runtime cycles.
 */
public class SequenceMarketSnapshotProvider implements MarketSnapshotProvider {

    private final Deque<MarketSnapshot> snapshots;
    private MarketSnapshot lastSnapshot;

    public SequenceMarketSnapshotProvider(List<MarketSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalArgumentException("snapshots must not be null or empty.");
        }
        this.snapshots = new ArrayDeque<>(snapshots);
    }

    @Override
    public MarketSnapshot load(TradingDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null.");
        }

        if (!snapshots.isEmpty()) {
            lastSnapshot = snapshots.removeFirst();
        }
        if (lastSnapshot == null) {
            throw new IllegalStateException("No market snapshot available for runtime " + definition.runtimeId());
        }

        return lastSnapshot;
    }
}
