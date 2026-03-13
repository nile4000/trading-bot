package ch.lueem.tradingbot.modes.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.runtime.RuntimeCycleResult;
import ch.lueem.tradingbot.core.time.Timeframes;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import ch.lueem.tradingbot.modes.backtest.model.Metadata;
import ch.lueem.tradingbot.modes.backtest.model.Report;
import ch.lueem.tradingbot.modes.backtest.model.Report.Position;
import jakarta.inject.Singleton;

/**
 * Generates the backtest report from shared runtime cycle results.
 */
@Singleton
public class ReportGenerator {

    private static final String EXECUTION_MODEL = "action_bar_close";
    private static final String POSITION_SIZING_MODEL = "all_in_spot";
    private static final int MONEY_SCALE = 4;
    private static final int DIVISION_SCALE = MONEY_SCALE + 4;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public Report assemble(BacktestConfig config, List<RuntimeCycleResult> cycleResults) {
        validateInputs(config, cycleResults);

        var positions = buildPositions(cycleResults);
        var lastCycle = cycleResults.getLast();
        var tradeStats = calculateTradeStats(positions);
        var exposure = calculateExposure(config, cycleResults);
        var performance = calculatePerformance(config, cycleResults, lastCycle);
        var metadata = buildMetadata(config, cycleResults, lastCycle);

        return new Report(
                metadata, tradeStats.closedTradeCount(), performance.initialCash(), performance.finalValue(),
                performance.totalReturnPercent(), performance.buyAndHoldReturnPercent(),
                performance.maxDrawdownPercent(), tradeStats.profitFactor(), tradeStats.winRatePercent(),
                tradeStats.averageWinningTrade(), tradeStats.averageLosingTrade(),
                exposure.timeInMarketDays(), exposure.exposurePercent(),
                lastCycle.portfolioSnapshot().position().open(), positions);
    }

    private void validateInputs(BacktestConfig config, List<RuntimeCycleResult> cycleResults) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null.");
        }
        if (cycleResults == null || cycleResults.isEmpty()) {
            throw new IllegalArgumentException("cycleResults must not be empty.");
        }
    }

    private List<Position> buildPositions(List<RuntimeCycleResult> cycleResults) {
        var positions = new ArrayList<Position>();
        PositionAccumulator openPosition = null;
        int positionNumber = 1;

        for (RuntimeCycleResult cycleResult : cycleResults) {
            if (!cycleResult.executionResult().executed())
                continue;

            if (cycleResult.action() == TradeAction.BUY) {
                openPosition = new PositionAccumulator(
                        cycleResult.marketSnapshot().observedAt().toString(),
                        cycleResult.marketSnapshot().lastPrice(), 
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

    private TradeStats calculateTradeStats(List<Position> positions) {
        var closedPositions = positions.stream()
                .filter(p -> "CLOSED".equals(p.status()))
                .toList();
        var winningTrades = closedPositions.stream()
                .filter(p -> p.profitLoss().signum() > 0)
                .toList();
        var losingTrades = closedPositions.stream()
                .filter(p -> p.profitLoss().signum() < 0)
                .toList();

        return new TradeStats(
                closedPositions.size(),
                calculateAverage(winningTrades),
                calculateAverage(losingTrades),
                calculateProfitFactor(winningTrades, losingTrades),
                calculateWinRatePercent(closedPositions.size(), winningTrades.size()));
    }

    private Exposure calculateExposure(BacktestConfig config, List<RuntimeCycleResult> cycleResults) {
        int timeInMarketBars = (int) cycleResults.stream()
                .filter(cycle -> cycle.portfolioSnapshot().position().open())
                .count();

        return new Exposure(
                calculateTimeInMarketDays(config.timeframe(), timeInMarketBars),
                calculateExposurePercent(cycleResults.size(), timeInMarketBars));
    }

    private Performance calculatePerformance(
            BacktestConfig config,
            List<RuntimeCycleResult> cycleResults,
            RuntimeCycleResult lastCycle) {
        var initialCash = scale(BigDecimal.valueOf(config.portfolio().initialCash()));
        var finalValue = calculateEquity(lastCycle);

        return new Performance(
                initialCash,
                finalValue,
                calculateReturnPercent(initialCash, finalValue),
                calculateBuyAndHoldReturnPercent(initialCash, cycleResults),
                calculateMaxDrawdownPercent(cycleResults));
    }

    private Metadata buildMetadata(BacktestConfig config, List<RuntimeCycleResult> cycleResults,
            RuntimeCycleResult lastCycle) {
        return new Metadata(
                BotMode.BACKTEST, config.symbol(), config.timeframe(), cycleResults.size(),
                cycleResults.getFirst().marketSnapshot().observedAt().toString(),
                lastCycle.marketSnapshot().observedAt().toString(),
                EXECUTION_MODEL, POSITION_SIZING_MODEL, config.strategy());
    }

    private Position buildClosedPositionReport(int positionNumber, PositionAccumulator openPosition,
            RuntimeCycleResult cycleResult) {
        var exitPrice = scale(cycleResult.marketSnapshot().lastPrice());
        var pnl = calculateProfitLoss(openPosition, exitPrice);
        var pnlPct = calculateProfitLossPercent(openPosition, pnl);

        return new Position(
                positionNumber, "CLOSED", openPosition.entryTime, scale(openPosition.entryPrice),
                cycleResult.marketSnapshot().observedAt().toString(), exitPrice,
                scale(openPosition.quantity), pnl, pnlPct);
    }

    private Position buildOpenPositionReport(int positionNumber, PositionAccumulator openPosition,
            RuntimeCycleResult lastCycle) {
        var lastPrice = scale(lastCycle.marketSnapshot().lastPrice());
        var pnl = calculateProfitLoss(openPosition, lastPrice);
        var pnlPct = calculateProfitLossPercent(openPosition, pnl);

        return new Position(
                positionNumber, "OPEN", openPosition.entryTime, scale(openPosition.entryPrice),
                null, null, scale(openPosition.quantity), pnl, pnlPct);
    }

    private BigDecimal calculateAverage(List<Position> trades) {
        if (trades.isEmpty())
            return scale(BigDecimal.ZERO);

        var total = trades.stream()
                .map(Position::profitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(trades.size()), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProfitFactor(List<Position> winningTrades,
            List<Position> losingTrades) {
        var grossProfit = winningTrades.stream()
                .map(Position::profitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var grossLoss = losingTrades.stream()
                .map(p -> p.profitLoss().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (grossLoss.signum() == 0) {
            return grossProfit.signum() == 0 ? scale(BigDecimal.ZERO) : new BigDecimal("9999.0000");
        }
        return grossProfit.divide(grossLoss, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateExposurePercent(int totalBars, int timeInMarketBars) {
        if (totalBars == 0) {
            return scale(BigDecimal.ZERO);
        }
        return scale(BigDecimal.valueOf(timeInMarketBars)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(totalBars), MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateTimeInMarketDays(String timeframe, int timeInMarketBars) {
        var barDuration = Timeframes.parse(timeframe);
        var investedSeconds = BigDecimal.valueOf(barDuration.toSeconds())
                .multiply(BigDecimal.valueOf(timeInMarketBars));
        return investedSeconds.divide(BigDecimal.valueOf(Duration.ofDays(1).toSeconds()), MONEY_SCALE,
                RoundingMode.HALF_UP);
    }

    private BigDecimal calculateReturnPercent(BigDecimal initialValue, BigDecimal finalValue) {
        if (initialValue.signum() == 0) {
            return scale(BigDecimal.ZERO);
        }
        return finalValue.subtract(initialValue)
                .divide(initialValue, DIVISION_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBuyAndHoldReturnPercent(BigDecimal initialCash, List<RuntimeCycleResult> cycleResults) {
        var firstPrice = cycleResults.getFirst().marketSnapshot().lastPrice();
        var lastPrice = cycleResults.getLast().marketSnapshot().lastPrice();

        if (firstPrice.signum() == 0) {
            return scale(BigDecimal.ZERO);
        }

        var benchmarkFinalValue = initialCash
                .divide(firstPrice, 16, RoundingMode.HALF_UP)
                .multiply(lastPrice);

        return calculateReturnPercent(initialCash, benchmarkFinalValue);
    }

    private BigDecimal calculateMaxDrawdownPercent(List<RuntimeCycleResult> cycleResults) {
        var peak = BigDecimal.ZERO;
        var maxDrawdown = BigDecimal.ZERO;

        for (RuntimeCycleResult cycleResult : cycleResults) {
            var equity = calculateEquity(cycleResult);
            if (equity.compareTo(peak) > 0) {
                peak = equity;
            }
            if (peak.signum() == 0) {
                continue;
            }

            var drawdownPercent = peak.subtract(equity)
                    .multiply(HUNDRED)
                    .divide(peak, DIVISION_SCALE, RoundingMode.HALF_UP);

            if (drawdownPercent.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdownPercent;
            }
        }
        return scale(maxDrawdown);
    }

    private BigDecimal calculateWinRatePercent(int closedTradeCount, int winningTradeCount) {
        if (closedTradeCount == 0) {
            return scale(BigDecimal.ZERO);
        }
        return BigDecimal.valueOf(winningTradeCount)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(closedTradeCount), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProfitLoss(PositionAccumulator openPosition, BigDecimal currentPrice) {
        var positionValueAtEntry = openPosition.quantity.multiply(openPosition.entryPrice);
        var currentPositionValue = openPosition.quantity.multiply(currentPrice);
        return scale(currentPositionValue.subtract(positionValueAtEntry));
    }

    private BigDecimal calculateProfitLossPercent(PositionAccumulator openPosition, BigDecimal pnl) {
        var positionValueAtEntry = openPosition.quantity.multiply(openPosition.entryPrice);
        if (positionValueAtEntry.signum() == 0) {
            return scale(BigDecimal.ZERO);
        }

        return pnl.multiply(HUNDRED)
                .divide(positionValueAtEntry, DIVISION_SCALE, RoundingMode.HALF_UP)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEquity(RuntimeCycleResult cycleResult) {
        var equity = cycleResult.portfolioSnapshot().availableCash();
        if (cycleResult.portfolioSnapshot().position().open()) {
            equity = equity.add(
                    cycleResult.portfolioSnapshot().position().quantity()
                            .multiply(cycleResult.marketSnapshot().lastPrice()));
        }
        return scale(equity);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private record PositionAccumulator(
            String entryTime,
            BigDecimal entryPrice,
            BigDecimal quantity) {
    }

    private record TradeStats(
            int closedTradeCount,
            BigDecimal averageWinningTrade,
            BigDecimal averageLosingTrade,
            BigDecimal profitFactor,
            BigDecimal winRatePercent) {
    }

    private record Exposure(
            BigDecimal timeInMarketDays,
            BigDecimal exposurePercent) {
    }

    private record Performance(
            BigDecimal initialCash,
            BigDecimal finalValue,
            BigDecimal totalReturnPercent,
            BigDecimal buyAndHoldReturnPercent,
            BigDecimal maxDrawdownPercent) {
    }
}
