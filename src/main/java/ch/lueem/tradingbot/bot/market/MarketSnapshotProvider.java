package ch.lueem.tradingbot.bot.market;

import ch.lueem.tradingbot.bot.model.BotDefinition;

/**
 * Provides the latest market snapshot for a bot cycle.
 */
public interface MarketSnapshotProvider {

    MarketSnapshot load(BotDefinition definition);
}
