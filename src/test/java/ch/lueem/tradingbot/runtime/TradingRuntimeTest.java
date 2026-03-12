package ch.lueem.tradingbot.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import ch.lueem.tradingbot.bot.market.MarketSnapshot;
import ch.lueem.tradingbot.bot.market.SequenceMarketSnapshotProvider;
import ch.lueem.tradingbot.bot.model.BotMode;
import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.execution.ExecutionService;
import ch.lueem.tradingbot.execution.ExecutionStatus;
import ch.lueem.tradingbot.integration.paper.StaticPortfolioService;
import ch.lueem.tradingbot.integration.simulation.SimulatedExecutionService;
import ch.lueem.tradingbot.integration.simulation.SimulatedPortfolioService;
import ch.lueem.tradingbot.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.strategy.signal.QueuedSignalEvaluator;
import ch.lueem.tradingbot.strategy.signal.TradeSignal;
import org.junit.jupiter.api.Test;

class TradingRuntimeTest {

    @Test
    void runCycle_returnsPostExecutionPortfolioSnapshot() {
        SimulatedPortfolioService portfolioService = new SimulatedPortfolioService();
        portfolioService.seedCash("BTCUSDT", new BigDecimal("1000.0000"));
        TradingRuntime runtime = new TradingRuntime(
                new TradingDefinition("bot-1", "v1", BotMode.PAPER, "BTCUSDT", "1m", new StrategyDefinition("queued_signals", null)),
                new SequenceMarketSnapshotProvider(List.of(
                        new MarketSnapshot(
                                "BTCUSDT",
                                "1m",
                                OffsetDateTime.parse("2026-03-12T10:15:30Z"),
                                new BigDecimal("100.00"),
                                List.of(new BigDecimal("100.00"))))),
                portfolioService,
                new QueuedSignalEvaluator(List.of(TradeSignal.BUY)),
                new SimulatedExecutionService(portfolioService));

        RuntimeCycleResult result = runtime.runCycle();

        assertTrue(result.executionResult().executed());
        assertTrue(result.portfolioSnapshot().position().open());
        assertEquals(BigDecimal.ZERO.setScale(4), result.portfolioSnapshot().availableCash());
        assertTrue(result.state().openPosition());
    }

    @Test
    void runCycle_rejectsMarketSnapshotWithWrongSymbol() {
        TradingRuntime runtime = new TradingRuntime(
                new TradingDefinition("bot-1", "v1", BotMode.PAPER, "BTCUSDT", "1m", new StrategyDefinition("queued_signals", null)),
                new SequenceMarketSnapshotProvider(List.of(
                        new MarketSnapshot(
                                "ETHUSDT",
                                "1m",
                                OffsetDateTime.parse("2026-03-12T10:15:30Z"),
                                new BigDecimal("100.00"),
                                List.of(new BigDecimal("100.00"))))),
                new SimulatedPortfolioService(),
                new QueuedSignalEvaluator(List.of(TradeSignal.HOLD)),
                new SimulatedExecutionService(new SimulatedPortfolioService()));

        IllegalStateException exception = assertThrows(IllegalStateException.class, runtime::runCycle);

        assertTrue(exception.getMessage().contains("symbol"));
    }

    @Test
    void runCycle_withValidationOnlyExecution_keepsPortfolioFlatAcrossSignals() {
        CapturingExecutionService executionService = new CapturingExecutionService();
        TradingRuntime runtime = new TradingRuntime(
                new TradingDefinition("bot-1", "v1", BotMode.PAPER, "BTCUSDT", "1m", new StrategyDefinition("queued_signals", null)),
                new SequenceMarketSnapshotProvider(List.of(
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:15:30Z"), new BigDecimal("100.00"), List.of(new BigDecimal("100.00"))),
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:16:30Z"), new BigDecimal("101.00"), List.of(new BigDecimal("100.00"), new BigDecimal("101.00"))),
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:17:30Z"), new BigDecimal("102.00"), List.of(new BigDecimal("100.00"), new BigDecimal("101.00"), new BigDecimal("102.00"))))),
                new StaticPortfolioService("BTCUSDT", new BigDecimal("1000.0000")),
                new QueuedSignalEvaluator(List.of(TradeSignal.BUY, TradeSignal.HOLD, TradeSignal.SELL)),
                executionService);

        RuntimeCycleResult first = runtime.runCycle();
        RuntimeCycleResult second = runtime.runCycle();
        RuntimeCycleResult third = runtime.runCycle();

        assertEquals(List.of(TradeSignal.BUY, TradeSignal.HOLD, TradeSignal.SELL), executionService.signals);
        assertFalse(first.portfolioSnapshot().position().open());
        assertFalse(second.portfolioSnapshot().position().open());
        assertFalse(third.portfolioSnapshot().position().open());
    }

    @Test
    void runCycle_usesSameExecutionFlowForLiveMode() {
        SimulatedPortfolioService portfolioService = new SimulatedPortfolioService();
        portfolioService.seedCash("BTCUSDT", new BigDecimal("1000.0000"));
        TradingRuntime runtime = new TradingRuntime(
                new TradingDefinition("runtime-live-1", "v1", BotMode.LIVE, "BTCUSDT", "1m", new StrategyDefinition("queued_signals", null)),
                new SequenceMarketSnapshotProvider(List.of(
                        new MarketSnapshot(
                                "BTCUSDT",
                                "1m",
                                OffsetDateTime.parse("2026-03-12T10:15:30Z"),
                                new BigDecimal("100.00"),
                                List.of(new BigDecimal("100.00"))))),
                portfolioService,
                new QueuedSignalEvaluator(List.of(TradeSignal.BUY)),
                new SimulatedExecutionService(portfolioService));

        RuntimeCycleResult result = runtime.runCycle();

        assertEquals(RuntimeStatus.IDLE, result.state().status());
        assertTrue(result.executionResult().executed());
        assertTrue(result.portfolioSnapshot().position().open());
    }

    private static final class CapturingExecutionService implements ExecutionService {
        private final java.util.ArrayList<TradeSignal> signals = new java.util.ArrayList<>();

        @Override
        public ExecutionResult execute(ch.lueem.tradingbot.execution.ExecutionRequest request) {
            signals.add(request.tradeSignal());
            return new ExecutionResult(ExecutionStatus.VALIDATED, false, false, "validated only");
        }
    }
}
