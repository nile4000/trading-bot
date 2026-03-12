package ch.lueem.tradingbot.bot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Provides market snapshots from an in-memory queue for deterministic bot runtime cycles.
 */
public class InMemoryMarketSnapshotProvider implements MarketSnapshotProvider {

    private final Deque<MarketSnapshot> snapshots;
    private MarketSnapshot lastSnapshot;

    public InMemoryMarketSnapshotProvider(List<MarketSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalArgumentException("snapshots must not be null or empty.");
        }
        this.snapshots = new ArrayDeque<>(snapshots);
    }

    @Override
    public MarketSnapshot load(BotDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null.");
        }

        if (!snapshots.isEmpty()) {
            lastSnapshot = snapshots.removeFirst();
        }
        if (lastSnapshot == null) {
            throw new IllegalStateException("No market snapshot available for bot " + definition.botId());
        }

        return lastSnapshot;
    }
}
