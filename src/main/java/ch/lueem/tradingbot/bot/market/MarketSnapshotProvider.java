package ch.lueem.tradingbot.bot.market;

import ch.lueem.tradingbot.runtime.TradingDefinition;

/**
 * Provides the latest market snapshot for a bot cycle.
 */
public interface MarketSnapshotProvider {

    MarketSnapshot load(TradingDefinition definition);
}
