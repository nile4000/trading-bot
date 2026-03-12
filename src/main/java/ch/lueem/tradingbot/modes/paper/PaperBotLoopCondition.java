package ch.lueem.tradingbot.modes.paper;

/**
 * Decides whether the paper bot should continue after a given number of
 * completed cycles.
 */
@FunctionalInterface
public interface PaperBotLoopCondition {

    boolean shouldContinue(int completedCycles);
}
