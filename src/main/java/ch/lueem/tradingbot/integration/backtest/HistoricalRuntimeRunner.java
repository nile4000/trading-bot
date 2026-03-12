package ch.lueem.tradingbot.integration.backtest;

import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.runtime.RuntimeCycleResult;
import ch.lueem.tradingbot.runtime.TradingRuntime;

/**
 * Runs the shared runtime deterministically across a fixed number of historical snapshots.
 */
public class HistoricalRuntimeRunner {

    public List<RuntimeCycleResult> run(TradingRuntime runtime, int cycleCount) {
        if (runtime == null) {
            throw new IllegalArgumentException("runtime must not be null.");
        }
        if (cycleCount <= 0) {
            throw new IllegalArgumentException("cycleCount must be greater than zero.");
        }

        List<RuntimeCycleResult> results = new ArrayList<>(cycleCount);
        for (int cycle = 0; cycle < cycleCount; cycle++) {
            results.add(runtime.runCycle());
        }
        return List.copyOf(results);
    }
}
