package ch.lueem.tradingbot.modes.paper;

import ch.lueem.tradingbot.adapters.config.LoggingConfig;
import ch.lueem.tradingbot.core.execution.ExecutionStatus;
import ch.lueem.tradingbot.core.runtime.RuntimeCycleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the paper bot continuously and emits technical lifecycle logs around
 * each cycle.
 */
public class PaperBotLoop {

    private static final Logger LOG = LoggerFactory.getLogger(PaperBotLoop.class);

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

    public void run(PaperBotSession session, LoggingConfig logging) {
        logStartup(logging, session);

        int completedCycles = 0;
        while (loopCondition.shouldContinue(completedCycles)) {
            RuntimeCycleResult tickResult = session.runtime().cycle();
            logCycle(logging, tickResult);
            completedCycles++;
            pause.sleep(session.paper().execution().tickIntervalMillis());
        }
    }

    private void logStartup(LoggingConfig logging, PaperBotSession session) {
        if (logging.lifecycleEvents()) {
            LOG.info(
                    "Starting paper bot. botId={}, symbol={}, timeframe={}, exchange={}, orderMode={}, restBaseUrl={}",
                    session.paper().bot().botId(),
                    session.paper().bot().symbol(),
                    session.paper().bot().timeframe(),
                    session.paper().execution().exchange(),
                    session.paper().execution().orderMode(),
                    session.restBaseUrl());
        }
    }

    private void logCycle(LoggingConfig logging, RuntimeCycleResult tickResult) {
        if (logging.lifecycleEvents()) {
            LOG.info(
                    "Paper tick. decision={}, execution={}, position={}, price={}, detail={}",
                    tickResult.action(),
                    executionLabel(tickResult),
                    positionLabel(tickResult),
                    tickResult.marketSnapshot().lastPrice().toPlainString(),
                    tickResult.executionResult().message());
        }
    }

    static String executionLabel(RuntimeCycleResult tickResult) {
        return tickResult.executionResult().status() == ExecutionStatus.EXECUTED
                ? "PLACED"
                : tickResult.executionResult().status().name();
    }

    static String positionLabel(RuntimeCycleResult tickResult) {
        return tickResult.portfolioSnapshot().position().open() ? "OPEN" : "FLAT";
    }
}
