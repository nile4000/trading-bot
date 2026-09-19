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

    private static final String EXECUTION_MODEL = "signal_bar_close_next_bar_open";
    private static final String POSITION_SIZING_MODEL = "fixed_quantity_spot";
    private static final int MONEY_SCALE = 4;
    private static final int DIVISION_SCALE = MONEY_SCALE + 4;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public Report assemble(BacktestConfig config, List<RuntimeCycleResult> cycleResults) {
        validateInputs(config, cycleResults);

        var positions = buildPositions(config, cycleResults);
        var lastCycle = cycleResults.getLast();
        var tradeStats = calculateTradeStats(positions);
        var financials = calculateFinancials(positions);
        var exposure = calculateExposure(config, cycleResults);
        var performance = calculatePerformance(config, cycleResults, lastCycle);
        var metadata = buildMetadata(config, cycleResults, lastCycle);

        return new Report(
                metadata, tradeStats.closedTradeCount(), performance.initialCash(), performance.finalValue(),
                financials.grossProfitLoss(), financials.netProfitLoss(), financials.fees(),
                financials.slippage(), financials.turnover(),
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

    private List<Position> buildPositions(BacktestConfig config, List<RuntimeCycleResult> cycleResults) {
        var positions = new ArrayList<Position>();
        PositionAccumulator openPosition = null;
        int positionNumber = 1;

        for (RuntimeCycleResult cycleResult : cycleResults) {
            if (!cycleResult.executionResult().executed()) {
                continue;
            }

            if (cycleResult.action() == TradeAction.BUY) {
                var quantity = cycleResult.portfolioSnapshot().position().quantity();
                var fillPrice = cycleResult.portfolioSnapshot().position().entryPrice();
                openPosition = new PositionAccumulator(
                        cycleResult.marketSnapshot().observedAt()
                                .minus(Timeframes.parse(config.timeframe())).toString(),
                        cycleResult.marketSnapshot().executionPrice(),
                        fillPrice,
                        quantity,
                        transactionFee(config, fillPrice, quantity));
            } else if (cycleResult.action() == TradeAction.SELL && openPosition != null) {
                positions.add(buildClosedPositionReport(positionNumber++, config, openPosition, cycleResult));
                openPosition = null;
            }
        }

        if (openPosition != null) {
            positions.add(buildOpenPositionReport(positionNumber, openPosition, cycleResults.getLast()));
        }

        return List.copyOf(positions);
    }

    private Financials calculateFinancials(List<Position> positions) {
        return new Financials(
                sum(positions, Position::grossProfitLoss),
                sum(positions, Position::profitLoss),
                sum(positions, Position::fees),
                sum(positions, Position::slippage),
                sum(positions, Position::turnover));
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

    private Position buildClosedPositionReport(int positionNumber, BacktestConfig config, PositionAccumulator openPosition,
            RuntimeCycleResult cycleResult) {
        var exitReferencePrice = cycleResult.marketSnapshot().executionPrice();
        var exitFillPrice = exitReferencePrice.multiply(BigDecimal.ONE.subtract(config.slippageRate()));
        var exitFee = transactionFee(config, exitFillPrice, openPosition.quantity);
        var grossPnl = openPosition.quantity.multiply(exitReferencePrice.subtract(openPosition.entryReferencePrice));
        var slippage = openPosition.quantity.multiply(
                openPosition.entryFillPrice.subtract(openPosition.entryReferencePrice)
                        .add(exitReferencePrice.subtract(exitFillPrice)));
        var fees = openPosition.entryFee.add(exitFee);
        var pnl = scale(grossPnl.subtract(fees).subtract(slippage));
        var turnover = openPosition.quantity.multiply(openPosition.entryFillPrice.add(exitFillPrice));
        var pnlPct = calculateProfitLossPercent(openPosition, pnl);

        return new Position(
                positionNumber, "CLOSED", openPosition.entryTime, scale(openPosition.entryFillPrice),
                cycleResult.marketSnapshot().observedAt()
                        .minus(Timeframes.parse(config.timeframe())).toString(), scale(exitFillPrice),
                scale(openPosition.quantity), pnl, pnlPct, scale(grossPnl), scale(fees),
                scale(slippage), scale(turnover));
    }

    private Position buildOpenPositionReport(int positionNumber, PositionAccumulator openPosition,
            RuntimeCycleResult lastCycle) {
        var lastPrice = scale(lastCycle.marketSnapshot().lastPrice());
        var grossPnl = openPosition.quantity.multiply(lastPrice.subtract(openPosition.entryReferencePrice));
        var slippage = openPosition.quantity.multiply(
                openPosition.entryFillPrice.subtract(openPosition.entryReferencePrice));
        var pnl = scale(grossPnl.subtract(openPosition.entryFee).subtract(slippage));
        var pnlPct = calculateProfitLossPercent(openPosition, pnl);
        var turnover = openPosition.quantity.multiply(openPosition.entryFillPrice);

        return new Position(
                positionNumber, "OPEN", openPosition.entryTime, scale(openPosition.entryFillPrice),
                null, null, scale(openPosition.quantity), pnl, pnlPct, scale(grossPnl),
                scale(openPosition.entryFee), scale(slippage), scale(turnover));
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

    private BigDecimal calculateProfitLossPercent(PositionAccumulator openPosition, BigDecimal pnl) {
        var entryCost = openPosition.quantity.multiply(openPosition.entryFillPrice).add(openPosition.entryFee);
        if (entryCost.signum() == 0) {
            return scale(BigDecimal.ZERO);
        }

        return pnl.multiply(HUNDRED)
                .divide(entryCost, DIVISION_SCALE, RoundingMode.HALF_UP)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal transactionFee(BacktestConfig config, BigDecimal fillPrice, BigDecimal quantity) {
        return fillPrice.multiply(quantity).multiply(config.executionFeeRate());
    }

    private BigDecimal sum(List<Position> positions, java.util.function.Function<Position, BigDecimal> value) {
        return scale(positions.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add));
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
            BigDecimal entryReferencePrice,
            BigDecimal entryFillPrice,
            BigDecimal quantity,
            BigDecimal entryFee) {
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

    private record Financials(
            BigDecimal grossProfitLoss,
            BigDecimal netProfitLoss,
            BigDecimal fees,
            BigDecimal slippage,
            BigDecimal turnover) {
    }
}
