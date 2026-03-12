package ch.lueem.tradingbot.backtest;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import ch.lueem.tradingbot.strategy.EmaCrossStrategyFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.backtest.BarSeriesManager;

/**
 * Coordinates CSV loading, strategy creation and result calculation for one backtest run.
 */
public class BacktestRunner {

    private final CsvBarSeriesLoader csvBarSeriesLoader;
    private final EmaCrossStrategyFactory strategyFactory;

    public BacktestRunner() {
        this(new CsvBarSeriesLoader(), new EmaCrossStrategyFactory());
    }

    public BacktestRunner(CsvBarSeriesLoader csvBarSeriesLoader, EmaCrossStrategyFactory strategyFactory) {
        this.csvBarSeriesLoader = csvBarSeriesLoader;
        this.strategyFactory = strategyFactory;
    }

    public BacktestReport run(
            Path csvPath,
            String symbol,
            String timeframe,
            int shortEma,
            int longEma,
            double initialCash) {
        validateInputs(symbol, timeframe, initialCash);

        Duration barDuration = parseTimeframe(timeframe);
        BarSeries series = csvBarSeriesLoader.load(csvPath, symbol + "-" + timeframe, barDuration);
        Strategy strategy = strategyFactory.create(series, shortEma, longEma);
        TradingRecord tradingRecord = new BarSeriesManager(series).run(strategy);

        int closedTradeCount = countClosedTrades(tradingRecord.getPositions());
        int winningTradeCount = countWinningTrades(tradingRecord.getPositions());
        double finalValue = simulateFinalValue(series, tradingRecord, initialCash);
        double returnPct = ((finalValue - initialCash) / initialCash) * 100.0;
        double winRatePct = closedTradeCount == 0 ? 0.0 : (winningTradeCount * 100.0) / closedTradeCount;

        return new BacktestReport(
                symbol,
                timeframe,
                series.getBarCount(),
                closedTradeCount,
                initialCash,
                finalValue,
                returnPct,
                winRatePct);
    }

    private void validateInputs(String symbol, String timeframe, double initialCash) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank.");
        }
        if (timeframe == null || timeframe.isBlank()) {
            throw new IllegalArgumentException("timeframe must not be blank.");
        }
        if (initialCash <= 0.0) {
            throw new IllegalArgumentException("initialCash must be greater than zero.");
        }
    }

    private Duration parseTimeframe(String timeframe) {
        return switch (timeframe) {
            case "1m" -> Duration.ofMinutes(1);
            case "5m" -> Duration.ofMinutes(5);
            case "15m" -> Duration.ofMinutes(15);
            case "1h" -> Duration.ofHours(1);
            case "4h" -> Duration.ofHours(4);
            case "1d" -> Duration.ofDays(1);
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        };
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

    private double simulateFinalValue(BarSeries series, TradingRecord tradingRecord, double initialCash) {
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

        if (units > 0.0 && !series.isEmpty()) {
            double lastClose = series.getLastBar().getClosePrice().doubleValue();
            return cash + (units * lastClose);
        }

        return cash;
    }
}
