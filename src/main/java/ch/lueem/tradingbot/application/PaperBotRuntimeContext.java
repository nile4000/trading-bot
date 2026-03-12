package ch.lueem.tradingbot.application;

import ch.lueem.tradingbot.runtime.TradingRuntime;

/**
 * Captures the fully wired runtime and static metadata for one paper bot process.
 */
public record PaperBotRuntimeContext(
        TradingRuntime runtime,
        PaperConfig paper,
        String restBaseUrl
) {
}
