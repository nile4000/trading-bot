package ch.lueem.tradingbot.core.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import ch.lueem.tradingbot.core.strategy.action.ActionContext;
import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

class StrategyEvaluatorParityTest {

    @Test
    void backtestAndPaperUseTheSameAdxDecisionLogic() {
        var definition = new StrategyDefinition("ema_cross", new StrategyParameters(1, 2));
        var factory = new StrategyEvaluatorFactory();

        var blockedBacktest = evaluator(factory, definition, new AdxFilterConfig(true, 1, 100));
        var blockedPaper = evaluator(factory, definition, new AdxFilterConfig(true, 1, 100));
        List<TradeAction> blockedBacktestActions = actions(
                blockedBacktest, List.of(false, false, false, false, false, true, true));
        List<TradeAction> blockedPaperActions = actions(
                blockedPaper, List.of(false, false, false, false, false, true, true));
        assertEquals(blockedBacktestActions, blockedPaperActions);
        assertEquals(TradeAction.HOLD, blockedBacktestActions.get(4));

        var allowedBacktest = evaluator(factory, definition, new AdxFilterConfig(true, 1, 0));
        var allowedPaper = evaluator(factory, definition, new AdxFilterConfig(true, 1, 0));
        List<TradeAction> expected = List.of(
                TradeAction.HOLD, TradeAction.HOLD, TradeAction.HOLD, TradeAction.HOLD,
                TradeAction.BUY, TradeAction.HOLD, TradeAction.SELL);
        assertEquals(expected, actions(allowedBacktest, List.of(false, false, false, false, false, true, true)));
        assertEquals(expected, actions(allowedPaper, List.of(false, false, false, false, false, true, true)));
    }

    private StrategyActionEvaluator evaluator(
            StrategyEvaluatorFactory factory,
            StrategyDefinition definition,
            AdxFilterConfig adxFilter) {
        BarSeries series = series();
        return factory.create(definition, StrategyEvaluatorContext.ta4j(series), adxFilter);
    }

    private List<TradeAction> actions(StrategyActionEvaluator evaluator, List<Boolean> openPositions) {
        return java.util.stream.IntStream.range(0, openPositions.size())
                .mapToObj(index -> evaluator.evaluate(new ActionContext(openPositions.get(index), index)))
                .toList();
    }

    private BarSeries series() {
        BarSeries series = new BaseBarSeriesBuilder().withName("parity").build();
        OffsetDateTime start = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        for (int index = 0; index < 7; index++) {
            String close = List.of("10", "10", "10", "9", "12", "13", "8").get(index);
            series.addBar(series.barBuilder()
                    .timePeriod(Duration.ofHours(1))
                    .endTime(start.plusHours(index).toInstant())
                    .openPrice(close)
                    .highPrice(close)
                    .lowPrice(close)
                    .closePrice(close)
                    .volume("1")
                    .build());
        }
        return series;
    }
}
