package ch.lueem.tradingbot.application;

import ch.lueem.tradingbot.runtime.RuntimeCycleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the paper bot loop and emits technical lifecycle logs around each cycle.
 */
public class PaperBotRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PaperBotRunner.class);

    private final PaperBotRunCondition runCondition;
    private final PaperBotSleeper sleeper;

    public PaperBotRunner() {
        this(
                completedCycles -> !Thread.currentThread().isInterrupted(),
                millis -> {
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                });
    }

    public PaperBotRunner(PaperBotRunCondition runCondition, PaperBotSleeper sleeper) {
        if (runCondition == null) {
            throw new IllegalArgumentException("runCondition must not be null.");
        }
        if (sleeper == null) {
            throw new IllegalArgumentException("sleeper must not be null.");
        }
        this.runCondition = runCondition;
        this.sleeper = sleeper;
    }

    public void run(PaperBotRuntimeContext context, LoggingConfig logging) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null.");
        }
        if (logging == null) {
            throw new IllegalArgumentException("logging must not be null.");
        }

        logStartup(logging, context);

        int completedCycles = 0;
        while (runCondition.shouldContinue(completedCycles)) {
            RuntimeCycleResult tickResult = context.runtime().cycle();
            logCycle(logging, tickResult);
            completedCycles++;
            sleeper.sleep(context.paper().execution().tickIntervalMillis());
        }
    }

    private void logStartup(LoggingConfig logging, PaperBotRuntimeContext context) {
        if (logging.lifecycleEvents()) {
            LOG.info(
                    "Starting paper bot. botId={}, symbol={}, timeframe={}, exchange={}, orderMode={}, restBaseUrl={}",
                    context.paper().bot().botId(),
                    context.paper().bot().symbol(),
                    context.paper().bot().timeframe(),
                    context.paper().execution().exchange(),
                    context.paper().execution().orderMode(),
                    context.restBaseUrl());
        }
    }

    private void logCycle(LoggingConfig logging, RuntimeCycleResult tickResult) {
        if (logging.lifecycleEvents()) {
            LOG.info(
                    "Paper cycle finished. status={}, action={}, executionStatus={}, executed={}, price={}, message={}",
                    tickResult.state().status(),
                    tickResult.action(),
                    tickResult.executionResult().status(),
                    tickResult.executionResult().executed(),
                    tickResult.marketSnapshot().lastPrice(),
                    tickResult.executionResult().message());
        }
    }
}
