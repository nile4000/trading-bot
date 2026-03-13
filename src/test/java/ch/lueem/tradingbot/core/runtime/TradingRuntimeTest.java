package ch.lueem.tradingbot.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import ch.lueem.tradingbot.core.execution.ExecutionService;
import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.execution.Result;
import ch.lueem.tradingbot.core.execution.Status;
import ch.lueem.tradingbot.adapters.portfolio.StaticPortfolioService;
import ch.lueem.tradingbot.adapters.execution.simulated.SimulatedExecutionService;
import ch.lueem.tradingbot.adapters.portfolio.SimulatedPortfolioService;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.action.QueuedActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.junit.jupiter.api.Test;

class TradingRuntimeTest {

    @Test
    void cycle_returnsPostExecutionPortfolioSnapshot() {
        SimulatedPortfolioService portfolioService = new SimulatedPortfolioService();
        portfolioService.seedCash("BTCUSDT", new BigDecimal("1000.0000"));
        TradingRuntime runtime = new TradingRuntime(
                queuedDefinition(),
                new SequenceMarketSnapshotProvider(List.of(
                        new MarketSnapshot(
                                "BTCUSDT",
                                "1m",
                                OffsetDateTime.parse("2026-03-12T10:15:30Z"),
                                new BigDecimal("100.00"),
                                List.of(new BigDecimal("100.00")),
                                0))),
                portfolioService,
                new QueuedActionEvaluator(List.of(TradeAction.BUY)),
                new SimulatedExecutionService(portfolioService));

        RuntimeCycleResult result = runtime.cycle();

        assertTrue(result.executionResult().executed());
        assertTrue(result.portfolioSnapshot().position().open());
        assertEquals(BigDecimal.ZERO.setScale(4), result.portfolioSnapshot().availableCash());
        assertTrue(result.portfolioSnapshot().position().open());
    }

    @Test
    void cycle_rejectsMarketSnapshotWithWrongSymbol() {
        TradingRuntime runtime = new TradingRuntime(
                queuedDefinition(),
                new SequenceMarketSnapshotProvider(List.of(
                        new MarketSnapshot(
                                "ETHUSDT",
                                "1m",
                                OffsetDateTime.parse("2026-03-12T10:15:30Z"),
                                new BigDecimal("100.00"),
                                List.of(new BigDecimal("100.00")),
                                0))),
                new SimulatedPortfolioService(),
                new QueuedActionEvaluator(List.of(TradeAction.HOLD)),
                new SimulatedExecutionService(new SimulatedPortfolioService()));

        IllegalStateException exception = assertThrows(IllegalStateException.class, runtime::cycle);

        assertTrue(exception.getMessage().contains("symbol"));
    }

    @Test
    void cycle_withValidationOnlyExecution_keepsPortfolioFlatAcrossActions() {
        CapturingExecutionService executionService = new CapturingExecutionService();
        TradingRuntime runtime = new TradingRuntime(
                queuedDefinition(),
                new SequenceMarketSnapshotProvider(List.of(
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:15:30Z"), new BigDecimal("100.00"), List.of(new BigDecimal("100.00")), 0),
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:16:30Z"), new BigDecimal("101.00"), List.of(new BigDecimal("100.00"), new BigDecimal("101.00")), 1),
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:17:30Z"), new BigDecimal("102.00"), List.of(new BigDecimal("100.00"), new BigDecimal("101.00"), new BigDecimal("102.00")), 2))),
                new StaticPortfolioService("BTCUSDT", new BigDecimal("1000.0000")),
                new QueuedActionEvaluator(List.of(TradeAction.BUY, TradeAction.HOLD, TradeAction.SELL)),
                executionService);

        RuntimeCycleResult first = runtime.cycle();
        RuntimeCycleResult second = runtime.cycle();
        RuntimeCycleResult third = runtime.cycle();

        assertEquals(List.of(TradeAction.BUY, TradeAction.HOLD, TradeAction.SELL), executionService.actions);
        assertFalse(first.portfolioSnapshot().position().open());
        assertFalse(second.portfolioSnapshot().position().open());
        assertFalse(third.portfolioSnapshot().position().open());
    }

    private TradingDefinition queuedDefinition() {
        return new TradingDefinition(
                "bot-1",
                "v1",
                BotMode.PAPER,
                "BTCUSDT",
                "1m",
                new StrategyDefinition("queued_actions", null));
    }

    private static final class CapturingExecutionService implements ExecutionService {
        private final java.util.ArrayList<TradeAction> actions = new java.util.ArrayList<>();

        @Override
        public Result execute(Request request) {
            actions.add(request.tradeAction());
            return new Result(Status.VALIDATED, false, false, "validated only");
        }
    }
}
