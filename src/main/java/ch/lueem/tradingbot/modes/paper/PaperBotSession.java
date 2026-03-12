package ch.lueem.tradingbot.modes.paper;

import ch.lueem.tradingbot.adapters.config.PaperConfig;
import ch.lueem.tradingbot.core.runtime.TradingRuntime;

/**
 * Captures the fully wired runtime and static metadata for one paper bot
 * session.
 */
public record PaperBotSession(
                TradingRuntime runtime,
                PaperConfig paper,
                String restBaseUrl) {
}
