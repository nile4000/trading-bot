package ch.lueem.tradingbot.application;

/**
 * Decides whether the paper bot loop should continue after a given number of completed cycles.
 */
@FunctionalInterface
public interface PaperBotRunCondition {

    boolean shouldContinue(int completedCycles);
}
