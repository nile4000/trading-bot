package ch.lueem.tradingbot.runtime;

/**
 * Provides the latest market snapshot for a bot cycle.
 */
public interface MarketSnapshotProvider {

    MarketSnapshot load(TradingDefinition definition);
}
