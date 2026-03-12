package ch.lueem.tradingbot.core.strategy.ta4j;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.core.strategy.action.StrategyActionEvaluator;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

class Ta4jStrategyFactoryTest {

    @Test
    void create_buildsTa4jEvaluatorForEmaCross() {
        Ta4jStrategyFactory factory = new Ta4jStrategyFactory();

        StrategyActionEvaluator evaluator = factory.create(
                new StrategyDefinition("ema_cross", new StrategyParameters(3, 7)),
                series());

        assertInstanceOf(Ta4jStrategyActionEvaluator.class, evaluator);
    }

    @Test
    void create_rejectsUnknownStrategyName() {
        Ta4jStrategyFactory factory = new Ta4jStrategyFactory();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> factory.create(new StrategyDefinition("rsi", new StrategyParameters(3, 7)), series()));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("Unsupported"));
    }

    private BarSeries series() {
        BarSeries series = new BaseBarSeriesBuilder().withName("factory-test").build();
        OffsetDateTime start = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        for (int index = 0; index < 8; index++) {
            String price = Integer.toString(100 + index);
            series.addBar(series.barBuilder()
                    .timePeriod(Duration.ofHours(1))
                    .endTime(start.plusHours(index).toInstant())
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
