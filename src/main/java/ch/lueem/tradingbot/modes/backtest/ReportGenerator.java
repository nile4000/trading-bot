package ch.lueem.tradingbot.modes.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.BacktestConfig;
import ch.lueem.tradingbot.modes.backtest.model.BacktestMetadata;
import ch.lueem.tradingbot.modes.backtest.model.BacktestPositionReport;
import ch.lueem.tradingbot.modes.backtest.model.BacktestReport;
import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.runtime.RuntimeCycleResult;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;

/**
 * Generates the backtest report from shared runtime cycle results.
 */
public class ReportGenerator {

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
        BigDecimal initialCash = scaleMoney(config.portfolio().initialCash());
        BigDecimal finalValue = calculateFinalValue(lastCycle);
        BigDecimal totalReturnPercent = calculateTotalReturnPercent(config, finalValue);
        BigDecimal winRatePercent = calculateWinRatePercent(closedTradeCount, winningTradeCount);
        BacktestMetadata metadata = buildMetadata(config, cycleResults, lastCycle);

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
                positions.add(buildClosedPositionReport(positionNumber++, openPosition, cycleResult));
                openPosition = null;
            }
        }

        if (openPosition != null) {
            positions.add(buildOpenPositionReport(positionNumber, openPosition, cycleResults.getLast()));
        }

        return List.copyOf(positions);
    }

    private BacktestMetadata buildMetadata(
            BacktestConfig config,
            List<RuntimeCycleResult> cycleResults,
            RuntimeCycleResult lastCycle) {
        return new BacktestMetadata(
                BotMode.BACKTEST,
                config.symbol(),
                config.timeframe(),
                cycleResults.size(),
                cycleResults.getFirst().marketSnapshot().observedAt().toString(),
                lastCycle.marketSnapshot().observedAt().toString(),
                EXECUTION_MODEL,
                POSITION_SIZING_MODEL,
                config.strategy());
    }

    private BacktestPositionReport buildClosedPositionReport(
            int positionNumber,
            PositionAccumulator openPosition,
            RuntimeCycleResult cycleResult) {
        BigDecimal exitPrice = roundToFourDecimals(cycleResult.marketSnapshot().lastPrice().doubleValue());
        BigDecimal pnl = calculateProfitLoss(openPosition, exitPrice);
        BigDecimal pnlPct = calculateProfitLossPercent(openPosition, pnl);
        return new BacktestPositionReport(
                positionNumber,
                "CLOSED",
                openPosition.entryTime,
                openPosition.entryPrice,
                cycleResult.marketSnapshot().observedAt().toString(),
                exitPrice,
                openPosition.quantity.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                pnl,
                pnlPct);
    }

    private BacktestPositionReport buildOpenPositionReport(
            int positionNumber,
            PositionAccumulator openPosition,
            RuntimeCycleResult lastCycle) {
        BigDecimal lastPrice = roundToFourDecimals(lastCycle.marketSnapshot().lastPrice().doubleValue());
        BigDecimal pnl = calculateProfitLoss(openPosition, lastPrice);
        BigDecimal pnlPct = calculateProfitLossPercent(openPosition, pnl);
        return new BacktestPositionReport(
                positionNumber,
                "OPEN",
                openPosition.entryTime,
                openPosition.entryPrice,
                null,
                null,
                openPosition.quantity.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                pnl,
                pnlPct);
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

    private BigDecimal calculateTotalReturnPercent(BacktestConfig config, BigDecimal finalValue) {
        return roundToFourDecimals(
                ((finalValue.doubleValue() - config.portfolio().initialCash()) / config.portfolio().initialCash()) * 100.0);
    }

    private BigDecimal calculateWinRatePercent(int closedTradeCount, int winningTradeCount) {
        if (closedTradeCount == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return roundToFourDecimals((winningTradeCount * 100.0) / closedTradeCount);
    }

    private BigDecimal calculateProfitLoss(PositionAccumulator openPosition, BigDecimal currentPrice) {
        BigDecimal positionValueAtEntry = openPosition.quantity.multiply(openPosition.entryPrice);
        return openPosition.quantity.multiply(currentPrice).subtract(positionValueAtEntry).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProfitLossPercent(PositionAccumulator openPosition, BigDecimal pnl) {
        BigDecimal positionValueAtEntry = openPosition.quantity.multiply(openPosition.entryPrice);
        if (positionValueAtEntry.signum() == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return roundToFourDecimals((pnl.doubleValue() / positionValueAtEntry.doubleValue()) * 100.0);
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

    private BigDecimal scaleMoney(double value) {
        return BigDecimal.valueOf(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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
