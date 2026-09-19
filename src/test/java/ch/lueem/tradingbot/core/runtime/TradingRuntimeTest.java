package ch.lueem.tradingbot.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.List;

import ch.lueem.tradingbot.core.execution.ExecutionService;
import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.execution.Result;
import ch.lueem.tradingbot.core.execution.Status;
import ch.lueem.tradingbot.adapters.execution.simulated.SimulatedExecutionService;
import ch.lueem.tradingbot.adapters.portfolio.SimulatedPortfolioService;
import ch.lueem.tradingbot.core.portfolio.PortfolioService;
import ch.lueem.tradingbot.core.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.core.portfolio.PositionSnapshot;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.action.QueuedActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.junit.jupiter.api.Test;
import org.ta4j.core.analysis.cost.ZeroCostModel;

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
                                new BigDecimal("100.00"),
                                0))),
                portfolioService,
                new QueuedActionEvaluator(List.of(TradeAction.BUY)),
                simulatedExecution(portfolioService));

        RuntimeCycleResult result = runtime.cycle();

        assertTrue(result.executionResult().executed());
        assertTrue(result.portfolioSnapshot().position().open());
        assertEquals(new BigDecimal("999.9900"), result.portfolioSnapshot().availableCash());
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
                                new BigDecimal("100.00"),
                                0))),
                new SimulatedPortfolioService(),
                new QueuedActionEvaluator(List.of(TradeAction.HOLD)),
                simulatedExecution(new SimulatedPortfolioService()));

        IllegalStateException exception = assertThrows(IllegalStateException.class, runtime::cycle);

        assertTrue(exception.getMessage().contains("symbol"));
    }

    private SimulatedExecutionService simulatedExecution(SimulatedPortfolioService portfolioService) {
        return new SimulatedExecutionService(
                portfolioService,
                new BigDecimal("0.0001"),
                BigDecimal.ZERO,
                new ZeroCostModel());
    }

    @Test
    void cycle_withValidationOnlyExecution_keepsPortfolioFlatAcrossActions() {
        CapturingExecutionService executionService = new CapturingExecutionService();
        TradingRuntime runtime = new TradingRuntime(
                queuedDefinition(),
                new SequenceMarketSnapshotProvider(List.of(
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:15:30Z"), new BigDecimal("100.00"), new BigDecimal("100.00"), 0),
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:16:30Z"), new BigDecimal("101.00"), new BigDecimal("101.00"), 1),
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:17:30Z"), new BigDecimal("102.00"), new BigDecimal("102.00"), 2))),
                staticPortfolio(new BigDecimal("1000.0000")),
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

    @Test
    void cycle_doesNotEvaluateTheSameCompletedBarTwice() {
        CapturingExecutionService executionService = new CapturingExecutionService();
        MarketSnapshot snapshot = new MarketSnapshot(
                "BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:15:00Z"),
                new BigDecimal("100.00"), new BigDecimal("100.00"), 0);
        TradingRuntime runtime = new TradingRuntime(
                queuedDefinition(),
                new SequenceMarketSnapshotProvider(List.of(snapshot)),
                staticPortfolio(new BigDecimal("1000")),
                new QueuedActionEvaluator(List.of(TradeAction.BUY, TradeAction.SELL)),
                executionService);

        runtime.cycle();
        RuntimeCycleResult repeated = runtime.cycle();

        assertEquals(List.of(TradeAction.BUY), executionService.actions);
        assertEquals(Status.SKIPPED, repeated.executionResult().status());
        assertEquals("bar_already_processed", repeated.executionResult().message());
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

    private PortfolioService staticPortfolio(BigDecimal availableCash) {
        PortfolioSnapshot snapshot = new PortfolioSnapshot(
                "BTCUSDT", availableCash, PositionSnapshot.flat());
        return symbol -> snapshot;
    }

    private static final class SequenceMarketSnapshotProvider implements MarketSnapshotProvider {
        private final ArrayDeque<MarketSnapshot> snapshots;
        private MarketSnapshot lastSnapshot;

        private SequenceMarketSnapshotProvider(List<MarketSnapshot> snapshots) {
            this.snapshots = new ArrayDeque<>(snapshots);
        }

        @Override
        public MarketSnapshot load(TradingDefinition definition) {
            if (!snapshots.isEmpty()) {
                lastSnapshot = snapshots.removeFirst();
            }
            return lastSnapshot;
        }
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
