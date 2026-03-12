package ch.lueem.tradingbot.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.strategy.EmaCrossStrategyFactory;
import ch.lueem.tradingbot.strategy.StrategyDefinition;
import ch.lueem.tradingbot.strategy.StrategyParameters;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.backtest.BarSeriesManager;

/**
 * Coordinates CSV loading, strategy creation and result calculation for one backtest run.
 */
public class BacktestRunner {

    private static final String EXECUTION_MODEL = "signal_bar_close";
    private static final String POSITION_SIZING_MODEL = "all_in_spot";

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

        int signalCount = tradingRecord.getTrades().size();
        int closedTradeCount = countClosedTrades(tradingRecord.getPositions());
        int winningTradeCount = countWinningTrades(tradingRecord.getPositions());
        Position currentPosition = tradingRecord.getCurrentPosition();
        boolean openPosition = currentPosition.isOpened();
        List<BacktestPositionReport> positionReports = buildPositionReports(series, tradingRecord, initialCash);
        PortfolioState portfolioState = simulatePortfolioState(series, tradingRecord, currentPosition, initialCash);
        double finalValue = portfolioState.finalValue();
        double returnPct = ((finalValue - initialCash) / initialCash) * 100.0;
        double winRatePct = closedTradeCount == 0 ? 0.0 : (winningTradeCount * 100.0) / closedTradeCount;
        BacktestMetadata metadata = new BacktestMetadata(
                symbol,
                timeframe,
                series.getBarCount(),
                series.getFirstBar().getEndTime().atOffset(ZoneOffset.UTC).toString(),
                series.getLastBar().getEndTime().atOffset(ZoneOffset.UTC).toString(),
                EXECUTION_MODEL,
                POSITION_SIZING_MODEL,
                new StrategyDefinition("ema_cross", new StrategyParameters(shortEma, longEma)));

        return new BacktestReport(
                metadata,
                signalCount,
                closedTradeCount,
                openPosition,
                roundToFourDecimals(initialCash),
                roundToFourDecimals(finalValue),
                roundToFourDecimals(returnPct),
                roundToFourDecimals(winRatePct),
                positionReports);
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

    private PortfolioState simulatePortfolioState(
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
            return new PortfolioState(cash + (units * lastClose), units);
        }

        return new PortfolioState(cash, 0.0);
    }

    private List<BacktestPositionReport> buildPositionReports(
            BarSeries series,
            TradingRecord tradingRecord,
            double initialCash) {
        List<BacktestPositionReport> reports = new ArrayList<>();
        double cash = initialCash;
        int positionNumber = 1;
        Position openPosition = tradingRecord.getCurrentPosition().isOpened() ? tradingRecord.getCurrentPosition() : null;

        for (Position position : tradingRecord.getPositions()) {
            if (position.getEntry() == null) {
                continue;
            }

            double entryPrice = position.getEntry().getPricePerAsset().doubleValue();
            double quantity = entryPrice > 0.0 ? cash / entryPrice : 0.0;
            double exitPrice = 0.0;
            double positionValueAtEntry = quantity * entryPrice;
            double pnl;
            double pnlPct;
            String status;
            String exitTime = null;
            BigDecimal exitPriceValue = null;

            if (position.isClosed() && position.getExit() != null) {
                status = "CLOSED";
                exitPrice = position.getExit().getPricePerAsset().doubleValue();
                exitTime = series.getBar(position.getExit().getIndex()).getEndTime().atOffset(ZoneOffset.UTC).toString();
                exitPriceValue = roundToFourDecimals(exitPrice);
                pnl = (quantity * exitPrice) - positionValueAtEntry;
                pnlPct = positionValueAtEntry == 0.0 ? 0.0 : (pnl / positionValueAtEntry) * 100.0;
                cash = quantity * exitPrice;
            } else {
                status = "OPEN";
                double lastClose = series.getLastBar().getClosePrice().doubleValue();
                pnl = (quantity * lastClose) - positionValueAtEntry;
                pnlPct = positionValueAtEntry == 0.0 ? 0.0 : (pnl / positionValueAtEntry) * 100.0;
            }

            reports.add(new BacktestPositionReport(
                    positionNumber++,
                    status,
                    series.getBar(position.getEntry().getIndex()).getEndTime().atOffset(ZoneOffset.UTC).toString(),
                    roundToFourDecimals(entryPrice),
                    exitTime,
                    exitPriceValue,
                    roundToFourDecimals(quantity),
                    roundToFourDecimals(pnl),
                    roundToFourDecimals(pnlPct)));
        }

        if (openPosition != null && openPosition.getEntry() != null) {
            double entryPrice = openPosition.getEntry().getPricePerAsset().doubleValue();
            double quantity = entryPrice > 0.0 ? cash / entryPrice : 0.0;
            double lastClose = series.getLastBar().getClosePrice().doubleValue();
            double positionValueAtEntry = quantity * entryPrice;
            double pnl = (quantity * lastClose) - positionValueAtEntry;
            double pnlPct = positionValueAtEntry == 0.0 ? 0.0 : (pnl / positionValueAtEntry) * 100.0;

            reports.add(new BacktestPositionReport(
                    positionNumber,
                    "OPEN",
                    series.getBar(openPosition.getEntry().getIndex()).getEndTime().atOffset(ZoneOffset.UTC).toString(),
                    roundToFourDecimals(entryPrice),
                    null,
                    null,
                    roundToFourDecimals(quantity),
                    roundToFourDecimals(pnl),
                    roundToFourDecimals(pnlPct)));
        }

        return reports;
    }

    private BigDecimal roundToFourDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private record PortfolioState(
            double finalValue,
            double openUnits) {
    }
}
