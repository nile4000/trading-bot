package ch.lueem.tradingbot.backtest.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.util.List;

import ch.lueem.tradingbot.bot.model.BotMode;
import ch.lueem.tradingbot.backtest.model.BacktestMetadata;
import ch.lueem.tradingbot.backtest.model.BacktestPositionReport;
import ch.lueem.tradingbot.backtest.model.BacktestReport;
import ch.lueem.tradingbot.strategy.definition.StrategyDefinition;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;

/**
 * Calculates summary metrics and metadata for one backtest run.
 */
public class BacktestMetricsCalculator {

    private static final String EXECUTION_MODEL = "signal_bar_close";
    private static final String POSITION_SIZING_MODEL = "all_in_spot";

    public BacktestReport calculate(
            BarSeries series,
            TradingRecord tradingRecord,
            String symbol,
            String timeframe,
            double initialCash,
            StrategyDefinition strategyDefinition,
            List<BacktestPositionReport> positionReports) {
        validateInputs(series, tradingRecord, symbol, timeframe, initialCash, strategyDefinition, positionReports);

        int executedSignalCount = tradingRecord.getTrades().size();
        int closedTradeCount = countClosedTrades(tradingRecord.getPositions());
        int winningTradeCount = countWinningTrades(tradingRecord.getPositions());
        Position currentPosition = tradingRecord.getCurrentPosition();
        boolean hasOpenPosition = currentPosition.isOpened();
        double finalValue = simulatePortfolioState(series, tradingRecord, currentPosition, initialCash);
        double totalReturnPercent = ((finalValue - initialCash) / initialCash) * 100.0;
        double winRatePercent = closedTradeCount == 0 ? 0.0 : (winningTradeCount * 100.0) / closedTradeCount;
        BacktestMetadata metadata = createMetadata(series, symbol, timeframe, strategyDefinition);

        return new BacktestReport(
                metadata,
                executedSignalCount,
                closedTradeCount,
                hasOpenPosition,
                roundToFourDecimals(initialCash),
                roundToFourDecimals(finalValue),
                roundToFourDecimals(totalReturnPercent),
                roundToFourDecimals(winRatePercent),
                positionReports);
    }

    private BacktestMetadata createMetadata(
            BarSeries series,
            String symbol,
            String timeframe,
            StrategyDefinition strategyDefinition) {
        return new BacktestMetadata(
                BotMode.BACKTEST,
                symbol,
                timeframe,
                series.getBarCount(),
                series.getFirstBar().getEndTime().atOffset(ZoneOffset.UTC).toString(),
                series.getLastBar().getEndTime().atOffset(ZoneOffset.UTC).toString(),
                EXECUTION_MODEL,
                POSITION_SIZING_MODEL,
                strategyDefinition);
    }

    private int countClosedTrades(List<Position> positions) {
        int count = 0;
        for (Position position : positions) {
            if (position.isClosed()) {
                count++;
            }
        }
        return count;
    }

    private int countWinningTrades(List<Position> positions) {
        int count = 0;
        for (Position position : positions) {
            if (position.isClosed() && position.hasProfit()) {
                count++;
            }
        }
        return count;
    }

    private double simulatePortfolioState(
            BarSeries series,
            TradingRecord tradingRecord,
            Position currentPosition,
            double initialCash) {
        double cash = initialCash;
        double units = 0.0;

        // V1 simplification: orders are filled on the close of the signal bar, which is
        // optimistic vs. live execution.
        for (Position position : tradingRecord.getPositions()) {
            if (position.getEntry() == null) {
                continue;
            }

            double entryPrice = position.getEntry().getPricePerAsset().doubleValue();
            if (cash > 0.0 && entryPrice > 0.0) {
                units = cash / entryPrice;
                cash = 0.0;
            }

            if (position.isClosed() && position.getExit() != null) {
                double exitPrice = position.getExit().getPricePerAsset().doubleValue();
                cash = units * exitPrice;
                units = 0.0;
            }
        }

        if (currentPosition.isOpened() && currentPosition.getEntry() != null) {
            double entryPrice = currentPosition.getEntry().getPricePerAsset().doubleValue();
            if (cash > 0.0 && entryPrice > 0.0) {
                units = cash / entryPrice;
                cash = 0.0;
            }
        }

        if (units > 0.0 && !series.isEmpty()) {
            double lastClose = series.getLastBar().getClosePrice().doubleValue();
            return cash + (units * lastClose);
        }

        return cash;
    }

    private void validateInputs(
            BarSeries series,
            TradingRecord tradingRecord,
            String symbol,
            String timeframe,
            double initialCash,
            StrategyDefinition strategyDefinition,
            List<BacktestPositionReport> positionReports) {
        if (series == null) {
            throw new IllegalArgumentException("series must not be null.");
        }
        if (tradingRecord == null) {
            throw new IllegalArgumentException("tradingRecord must not be null.");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank.");
        }
        if (timeframe == null || timeframe.isBlank()) {
            throw new IllegalArgumentException("timeframe must not be blank.");
        }
        if (initialCash <= 0.0) {
            throw new IllegalArgumentException("initialCash must be greater than zero.");
        }
        if (strategyDefinition == null) {
            throw new IllegalArgumentException("strategyDefinition must not be null.");
        }
        if (positionReports == null) {
            throw new IllegalArgumentException("positionReports must not be null.");
        }
    }

    private BigDecimal roundToFourDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(4, RoundingMode.HALF_UP);
    }
}
