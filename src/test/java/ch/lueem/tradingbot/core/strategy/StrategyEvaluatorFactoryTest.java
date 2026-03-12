package ch.lueem.tradingbot.core.strategy;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import ch.lueem.tradingbot.core.strategy.action.QueuedActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import ch.lueem.tradingbot.core.strategy.ta4j.Ta4jStrategyActionEvaluator;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

class StrategyEvaluatorFactoryTest {

    @Test
    void create_buildsQueuedEvaluator() {
        StrategyEvaluatorFactory factory = new StrategyEvaluatorFactory();

        StrategyActionEvaluator evaluator = factory.create(
                new StrategyDefinition("queued_actions", null),
                StrategyEvaluatorContext.queued(List.of(TradeAction.BUY, TradeAction.SELL)));

        assertInstanceOf(QueuedActionEvaluator.class, evaluator);
    }

    @Test
    void create_buildsTa4jEvaluatorForEmaCross() {
        StrategyEvaluatorFactory factory = new StrategyEvaluatorFactory();

        StrategyActionEvaluator evaluator = factory.create(
                new StrategyDefinition("ema_cross", new StrategyParameters(3, 7)),
                StrategyEvaluatorContext.ta4j(series()));

        assertInstanceOf(Ta4jStrategyActionEvaluator.class, evaluator);
    }

    @Test
    void create_rejectsUnknownName() {
        StrategyEvaluatorFactory factory = new StrategyEvaluatorFactory();

        assertThrows(
                IllegalStateException.class,
                () -> factory.create(new StrategyDefinition("unknown", null), StrategyEvaluatorContext.queued(List.of())));
    }

    private BarSeries series() {
        BarSeries series = new BaseBarSeriesBuilder().withName("strategy-factory-test").build();
        OffsetDateTime start = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        for (int index = 0; index < 8; index++) {
            String price = Integer.toString(100 + index);
            series.addBar(series.barBuilder()
                    .timePeriod(Duration.ofMinutes(1))
                    .endTime(start.plusMinutes(index).toInstant())
                    .openPrice(price)
                    .highPrice(price)
                    .lowPrice(price)
                    .closePrice(price)
                    .volume("1")
                    .build());
        }
        return series;
    }
}
