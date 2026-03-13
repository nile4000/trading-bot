package ch.lueem.tradingbot.modes.backtest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.adapters.config.backtest.PortfolioConfig;
import ch.lueem.tradingbot.core.execution.Result;
import ch.lueem.tradingbot.core.execution.Status;
import ch.lueem.tradingbot.core.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.core.portfolio.PositionSnapshot;
import ch.lueem.tradingbot.core.runtime.MarketSnapshot;
import ch.lueem.tradingbot.core.runtime.RuntimeCycleResult;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.definition.StrategyParameters;
import org.junit.jupiter.api.Test;

class ReportGeneratorPrecisionTest {

    @Test
    void assemble_keepsFourDecimalPercentMetricsWithSharedDivisionScale() {
        ReportGenerator generator = new ReportGenerator();
        BacktestConfig config = new BacktestConfig(
                java.nio.file.Path.of("data/historical/BTCUSDT-1h.csv"),
                "BTCUSDT",
                "1h",
                new StrategyDefinition("ema_cross", new StrategyParameters(3, 7)),
                new PortfolioConfig(10000.0));

        List<RuntimeCycleResult> cycleResults = List.of(
                cycle(
                        "2026-01-01T00:00:00Z",
                        "3.00000000",
                        "0.0000",
                        true,
                        "3333.33333333",
                        "3.00000000",
                        TradeAction.BUY,
                        true),
                cycle(
                        "2026-01-01T01:00:00Z",
                        "3.33333333",
                        "0.0000",
                        true,
                        "3333.33333333",
                        "3.00000000",
                        TradeAction.HOLD,
                        false),
                cycle(
                        "2026-01-01T02:00:00Z",
                        "2.66666667",
                        "8888.8889",
                        false,
                        null,
                        null,
                        TradeAction.SELL,
                        true));

        var report = generator.assemble(config, cycleResults);

        assertEquals("-11.1111", report.buyAndHoldReturnPercent().toPlainString());
        assertEquals("20.0000", report.maxDrawdownPercent().toPlainString());
        assertEquals("-1111.0000", report.averageLosingTrade().toPlainString());
        assertEquals("-11.1111", report.totalReturnPercent().toPlainString());
    }

    private RuntimeCycleResult cycle(
            String observedAt,
            String lastPrice,
            String availableCash,
            boolean openPosition,
            String quantity,
            String entryPrice,
            TradeAction action,
            boolean executed) {
        return new RuntimeCycleResult(
                new MarketSnapshot(
                        "BTCUSDT",
                        "1h",
                        OffsetDateTime.parse(observedAt),
                        new BigDecimal(lastPrice),
                        List.of(new BigDecimal(lastPrice)),
                        0),
                new PortfolioSnapshot(
                        "BTCUSDT",
                        new BigDecimal(availableCash),
                        openPosition
                                ? new PositionSnapshot(true, new BigDecimal(quantity), new BigDecimal(entryPrice), OffsetDateTime.parse(observedAt))
                                : PositionSnapshot.flat()),
                action,
                new Result(executed ? Status.EXECUTED : Status.SKIPPED, executed, openPosition, "test"));
    }
}
