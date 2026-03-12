package ch.lueem.tradingbot.modes.paper;

/**
 * Suspends the paper bot between cycles.
 */
@FunctionalInterface
public interface PaperBotPause {

    void sleep(long millis);
}
