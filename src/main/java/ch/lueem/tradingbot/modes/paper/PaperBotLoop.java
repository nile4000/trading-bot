package ch.lueem.tradingbot.modes.paper;

import ch.lueem.tradingbot.core.execution.Status;
import ch.lueem.tradingbot.core.runtime.RuntimeCycleResult;
import org.jboss.logging.Logger;

/**
 * Runs the paper bot continuously and emits technical lifecycle logs around
 * each cycle.
 */
public class PaperBotLoop {

    private static final Logger LOG = Logger.getLogger(PaperBotLoop.class);

    private final PaperBotLoopCondition loopCondition;
    private final PaperBotPause pause;

    public PaperBotLoop() {
        this(completedCycles -> !Thread.currentThread().isInterrupted(),
                millis -> {
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                });
    }

    public PaperBotLoop(PaperBotLoopCondition loopCondition, PaperBotPause pause) {
        this.loopCondition = loopCondition;
        this.pause = pause;
    }

    public void run(PaperBotSession session, boolean lifecycleEvents) {
        logStartup(lifecycleEvents, session);

        int completedCycles = 0;
        while (loopCondition.shouldContinue(completedCycles)) {
            RuntimeCycleResult tickResult = session.runtime().cycle();
            logCycle(lifecycleEvents, tickResult);
            completedCycles++;
            pause.sleep(session.paper().execution().tickIntervalMillis());
        }
    }

    private void logStartup(boolean lifecycleEvents, PaperBotSession session) {
        if (lifecycleEvents) {
            LOG.infof(
                    "Starting paper bot. botId=%s, symbol=%s, timeframe=%s, exchange=%s, orderMode=%s, restBaseUrl=%s",
                    session.paper().bot().botId(),
                    session.paper().bot().symbol(),
                    session.paper().bot().timeframe(),
                    session.paper().execution().exchange(),
                    session.paper().execution().orderMode(),
                    session.restBaseUrl());
        }
    }

    private void logCycle(boolean lifecycleEvents, RuntimeCycleResult tickResult) {
        if (lifecycleEvents) {
            LOG.infof(
                    "Paper tick. decision=%s, execution=%s, position=%s, price=%s, detail=%s",
                    tickResult.action(),
                    executionLabel(tickResult),
                    positionLabel(tickResult),
                    tickResult.marketSnapshot().lastPrice().toPlainString(),
                    tickResult.executionResult().message());
        }
    }

    static String executionLabel(RuntimeCycleResult tickResult) {
        return tickResult.executionResult().status() == Status.EXECUTED
                ? "PLACED"
                : tickResult.executionResult().status().name();
    }

    static String positionLabel(RuntimeCycleResult tickResult) {
        return tickResult.portfolioSnapshot().position().open() ? "OPEN" : "FLAT";
    }
}
