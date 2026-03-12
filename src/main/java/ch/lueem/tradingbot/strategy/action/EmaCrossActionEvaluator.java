package ch.lueem.tradingbot.strategy.action;

import java.math.BigDecimal;
import java.util.List;

import ch.lueem.tradingbot.strategy.definition.StrategyParameters;

/**
 * Evaluates EMA-cross actions from close-price history for backtest and runtime flows.
 */
public class EmaCrossActionEvaluator implements StrategyActionEvaluator {

    private final int shortLength;
    private final int longLength;

    public EmaCrossActionEvaluator(StrategyParameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters must not be null.");
        }
        if (parameters.shortEma() <= 0 || parameters.longEma() <= 0) {
            throw new IllegalArgumentException("EMA lengths must be greater than zero.");
        }
        if (parameters.shortEma() >= parameters.longEma()) {
            throw new IllegalArgumentException("shortEma must be smaller than longEma.");
        }
        this.shortLength = parameters.shortEma();
        this.longLength = parameters.longEma();
    }

    @Override
    public TradeAction evaluate(ActionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null.");
        }

        List<BigDecimal> history = context.closePriceHistory();
        if (history == null || history.size() <= longLength) {
            return TradeAction.HOLD;
        }

        double previousShort = calculateEma(history, shortLength, history.size() - 1);
        double previousLong = calculateEma(history, longLength, history.size() - 1);
        double currentShort = calculateEma(history, shortLength, history.size());
        double currentLong = calculateEma(history, longLength, history.size());

        if (!context.openPosition() && previousShort <= previousLong && currentShort > currentLong) {
            return TradeAction.BUY;
        }
        if (context.openPosition() && previousShort >= previousLong && currentShort < currentLong) {
            return TradeAction.SELL;
        }
        return TradeAction.HOLD;
    }

    private double calculateEma(List<BigDecimal> history, int length, int itemCount) {
        double smoothing = 2.0 / (length + 1.0);
        double ema = history.getFirst().doubleValue();
        for (int index = 1; index < itemCount; index++) {
            double price = history.get(index).doubleValue();
            ema = (price * smoothing) + (ema * (1.0 - smoothing));
        }
        return ema;
    }
}
