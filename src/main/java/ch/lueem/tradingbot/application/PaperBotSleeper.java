package ch.lueem.tradingbot.application;

/**
 * Suspends the paper bot loop between cycles.
 */
@FunctionalInterface
public interface PaperBotSleeper {

    void sleep(long millis);
}
