package ch.lueem.tradingbot.core.strategy.definition;

/**
 * Holds the parameter set for one strategy definition.
 */
public record StrategyParameters(
        Integer shortEma,
        Integer longEma,
        Integer rsiPeriod,
        Integer buyBelow,
        Integer sellAbove
) {
    public StrategyParameters(Integer shortEma, Integer longEma) {
        this(shortEma, longEma, null, null, null);
    }

    public static StrategyParameters rsiReversion(int rsiPeriod, int buyBelow, int sellAbove) {
        return new StrategyParameters(null, null, rsiPeriod, buyBelow, sellAbove);
    }
}
