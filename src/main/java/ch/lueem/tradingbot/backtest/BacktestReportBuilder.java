package ch.lueem.tradingbot.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.application.BacktestConfig;
import ch.lueem.tradingbot.backtest.model.BacktestMetadata;
import ch.lueem.tradingbot.backtest.model.BacktestPositionReport;
import ch.lueem.tradingbot.backtest.model.BacktestReport;
import ch.lueem.tradingbot.bot.model.BotMode;
import ch.lueem.tradingbot.runtime.RuntimeCycleResult;
import ch.lueem.tradingbot.strategy.action.TradeAction;

/**
 * Builds the backtest report from shared runtime cycle results.
 */
public class BacktestReportBuilder {

    private static final String EXECUTION_MODEL = "action_bar_close";
    private static final String POSITION_SIZING_MODEL = "all_in_spot";
    private static final int MONEY_SCALE = 4;

    public BacktestReport assemble(BacktestConfig config, List<RuntimeCycleResult> cycleResults) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null.");
        }
        if (cycleResults == null || cycleResults.isEmpty()) {
            throw new IllegalArgumentException("cycleResults must not be empty.");
        }

        List<BacktestPositionReport> positions = buildPositions(cycleResults);
        RuntimeCycleResult lastCycle = cycleResults.getLast();
        int executedSignalCount = countExecutedSignals(cycleResults);
        int closedTradeCount = countClosedTrades(positions);
        int winningTradeCount = countWinningTrades(positions);
        BigDecimal initialCash = BigDecimal.valueOf(config.portfolio().initialCash()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal finalValue = calculateFinalValue(lastCycle);
        BigDecimal totalReturnPercent = roundToFourDecimals(
                ((finalValue.doubleValue() - config.portfolio().initialCash()) / config.portfolio().initialCash()) * 100.0);
        BigDecimal winRatePercent = closedTradeCount == 0
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : roundToFourDecimals((winningTradeCount * 100.0) / closedTradeCount);

        BacktestMetadata metadata = new BacktestMetadata(
                BotMode.BACKTEST,
                config.symbol(),
                config.timeframe(),
                cycleResults.size(),
                cycleResults.getFirst().marketSnapshot().observedAt().toString(),
                lastCycle.marketSnapshot().observedAt().toString(),
                EXECUTION_MODEL,
                POSITION_SIZING_MODEL,
                config.strategy());

        return new BacktestReport(
                metadata,
                executedSignalCount,
                closedTradeCount,
                lastCycle.portfolioSnapshot().position().open(),
                initialCash,
                finalValue,
                totalReturnPercent,
                winRatePercent,
                positions);
    }

    private List<BacktestPositionReport> buildPositions(List<RuntimeCycleResult> cycleResults) {
        List<BacktestPositionReport> positions = new ArrayList<>();
        PositionAccumulator openPosition = null;
        int positionNumber = 1;

        for (RuntimeCycleResult cycleResult : cycleResults) {
            if (!cycleResult.executionResult().executed()) {
                continue;
            }

            if (cycleResult.action() == TradeAction.BUY) {
                openPosition = new PositionAccumulator(
                        cycleResult.marketSnapshot().observedAt().toString(),
                        roundToFourDecimals(cycleResult.marketSnapshot().lastPrice().doubleValue()),
                        cycleResult.portfolioSnapshot().position().quantity());
                continue;
            }

            if (cycleResult.action() == TradeAction.SELL && openPosition != null) {
                BigDecimal exitPrice = roundToFourDecimals(cycleResult.marketSnapshot().lastPrice().doubleValue());
                BigDecimal positionValueAtEntry = openPosition.quantity.multiply(openPosition.entryPrice);
                BigDecimal pnl = openPosition.quantity.multiply(exitPrice).subtract(positionValueAtEntry).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                BigDecimal pnlPct = positionValueAtEntry.signum() == 0
                        ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                        : roundToFourDecimals((pnl.doubleValue() / positionValueAtEntry.doubleValue()) * 100.0);
                positions.add(new BacktestPositionReport(
                        positionNumber++,
                        "CLOSED",
                        openPosition.entryTime,
                        openPosition.entryPrice,
                        cycleResult.marketSnapshot().observedAt().toString(),
                        exitPrice,
                        openPosition.quantity.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                        pnl,
                        pnlPct));
                openPosition = null;
            }
        }

        if (openPosition != null) {
            RuntimeCycleResult lastCycle = cycleResults.getLast();
            BigDecimal lastPrice = roundToFourDecimals(lastCycle.marketSnapshot().lastPrice().doubleValue());
            BigDecimal positionValueAtEntry = openPosition.quantity.multiply(openPosition.entryPrice);
            BigDecimal pnl = openPosition.quantity.multiply(lastPrice).subtract(positionValueAtEntry).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal pnlPct = positionValueAtEntry.signum() == 0
                    ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                    : roundToFourDecimals((pnl.doubleValue() / positionValueAtEntry.doubleValue()) * 100.0);
            positions.add(new BacktestPositionReport(
                    positionNumber,
                    "OPEN",
                    openPosition.entryTime,
                    openPosition.entryPrice,
                    null,
                    null,
                    openPosition.quantity.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    pnl,
                    pnlPct));
        }

        return List.copyOf(positions);
    }

    private int countExecutedSignals(List<RuntimeCycleResult> cycleResults) {
        int count = 0;
        for (RuntimeCycleResult cycleResult : cycleResults) {
            if (cycleResult.executionResult().executed()) {
                count++;
            }
        }
        return count;
    }

    private int countClosedTrades(List<BacktestPositionReport> positions) {
        int count = 0;
        for (BacktestPositionReport position : positions) {
            if ("CLOSED".equals(position.status())) {
                count++;
            }
        }
        return count;
    }

    private int countWinningTrades(List<BacktestPositionReport> positions) {
        int count = 0;
        for (BacktestPositionReport position : positions) {
            if ("CLOSED".equals(position.status()) && position.profitLoss().signum() > 0) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal calculateFinalValue(RuntimeCycleResult lastCycle) {
        if (lastCycle.portfolioSnapshot().position().open()) {
            return roundToFourDecimals(
                    lastCycle.portfolioSnapshot().position().quantity()
                            .multiply(lastCycle.marketSnapshot().lastPrice())
                            .doubleValue());
        }
        return lastCycle.portfolioSnapshot().availableCash().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal roundToFourDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private record PositionAccumulator(
            String entryTime,
            BigDecimal entryPrice,
            BigDecimal quantity
    ) {
    }
}
