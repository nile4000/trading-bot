package ch.lueem.tradingbot.core.strategy.ta4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import ch.lueem.tradingbot.core.strategy.action.ActionContext;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Rule;

class Ta4jStrategyActionEvaluatorTest {

    @Test
    void evaluate_returnsBuyWhenShortEmaCrossesAboveLongEmaWithoutOpenPosition() {
        BarSeries series = series("10", "10", "10", "9", "12");
        Ta4jStrategyActionEvaluator evaluator = new Ta4jStrategyActionEvaluator(
                series,
                satisfiedOnlyAt(4),
                neverSatisfied(),
                3);

        TradeAction action = evaluator.evaluate(context(series, 4, false));

        assertEquals(TradeAction.BUY, action);
    }

    @Test
    void evaluate_returnsSellWhenShortEmaCrossesBelowLongEmaWithOpenPosition() {
        BarSeries series = series("10", "10", "10", "12", "9");
        Ta4jStrategyActionEvaluator evaluator = new Ta4jStrategyActionEvaluator(
                series,
                neverSatisfied(),
                satisfiedOnlyAt(4),
                3);

        TradeAction action = evaluator.evaluate(context(series, 4, true));

        assertEquals(TradeAction.SELL, action);
    }

    @Test
    void evaluate_returnsHoldWhenNoCrossIsPresent() {
        BarSeries series = series("10", "11", "12", "13", "14");
        Ta4jStrategyActionEvaluator evaluator = new Ta4jStrategyActionEvaluator(
                series,
                neverSatisfied(),
                neverSatisfied(),
                3);

        TradeAction action = evaluator.evaluate(context(series, 4, false));

        assertEquals(TradeAction.HOLD, action);
    }

    @Test
    void evaluate_returnsHoldWhileSeriesIsStillWarmingUp() {
        BarSeries series = series("10", "10", "10", "9", "12");
        Ta4jStrategyActionEvaluator evaluator = new Ta4jStrategyActionEvaluator(
                series,
                satisfiedOnlyAt(2),
                neverSatisfied(),
                3);

        TradeAction action = evaluator.evaluate(context(series, 2, false));

        assertEquals(TradeAction.HOLD, action);
    }

    private ActionContext context(BarSeries series, int barIndex, boolean openPosition) {
        BigDecimal lastPrice = BigDecimal.valueOf(series.getBar(barIndex).getClosePrice().doubleValue());
        return new ActionContext(
                "BTCUSDT",
                "1h",
                OffsetDateTime.parse("2026-01-01T00:00:00Z").plusHours(barIndex),
                lastPrice,
                openPosition,
                List.of(lastPrice),
                barIndex,
                series);
    }

    private Rule satisfiedOnlyAt(int targetIndex) {
        return (index, tradingRecord) -> index == targetIndex;
    }

    private Rule neverSatisfied() {
        return (index, tradingRecord) -> false;
    }

    private BarSeries series(String... closePrices) {
        BarSeries series = new BaseBarSeriesBuilder().withName("ema-cross-test").build();
        OffsetDateTime start = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        for (int index = 0; index < closePrices.length; index++) {
            String closePrice = closePrices[index];
            series.addBar(series.barBuilder()
                    .timePeriod(Duration.ofHours(1))
                    .endTime(start.plusHours(index).toInstant())
                    .openPrice(closePrice)
                    .highPrice(closePrice)
                    .lowPrice(closePrice)
                    .closePrice(closePrice)
                    .volume("1")
                    .build());
        }
        return series;
    }
}
