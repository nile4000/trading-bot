package ch.lueem.tradingbot.backtest.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import ch.lueem.tradingbot.backtest.model.BacktestPositionReport;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;

/**
 * Builds position-level backtest reports from the ta4j trading record.
 */
public class BacktestPositionReportBuilder {

    public List<BacktestPositionReport> build(BarSeries series, TradingRecord tradingRecord, double initialCash) {
        validateInputs(series, tradingRecord, initialCash);

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
            double positionValueAtEntry = quantity * entryPrice;

            if (position.isClosed() && position.getExit() != null) {
                double exitPrice = position.getExit().getPricePerAsset().doubleValue();
                double pnl = (quantity * exitPrice) - positionValueAtEntry;
                double pnlPct = positionValueAtEntry == 0.0 ? 0.0 : (pnl / positionValueAtEntry) * 100.0;
                cash = quantity * exitPrice;

                reports.add(new BacktestPositionReport(
                        positionNumber++,
                        "CLOSED",
                        series.getBar(position.getEntry().getIndex()).getEndTime().atOffset(ZoneOffset.UTC).toString(),
                        roundToFourDecimals(entryPrice),
                        series.getBar(position.getExit().getIndex()).getEndTime().atOffset(ZoneOffset.UTC).toString(),
                        roundToFourDecimals(exitPrice),
                        roundToFourDecimals(quantity),
                        roundToFourDecimals(pnl),
                        roundToFourDecimals(pnlPct)));
            }
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

    private void validateInputs(BarSeries series, TradingRecord tradingRecord, double initialCash) {
        if (series == null) {
            throw new IllegalArgumentException("series must not be null.");
        }
        if (tradingRecord == null) {
            throw new IllegalArgumentException("tradingRecord must not be null.");
        }
        if (initialCash <= 0.0) {
            throw new IllegalArgumentException("initialCash must be greater than zero.");
        }
    }

    private BigDecimal roundToFourDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(4, RoundingMode.HALF_UP);
    }
}
