package ch.lueem.tradingbot.modes.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ch.lueem.tradingbot.adapters.config.paper.BinanceConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperBotConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperExchange;
import ch.lueem.tradingbot.adapters.config.paper.PaperExecutionConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperOrderMode;
import ch.lueem.tradingbot.adapters.config.paper.PaperStrategyConfig;
import ch.lueem.tradingbot.adapters.portfolio.StaticPortfolioService;
import ch.lueem.tradingbot.core.runtime.BotMode;
import ch.lueem.tradingbot.core.execution.ExecutionService;
import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.execution.Result;
import ch.lueem.tradingbot.core.execution.Status;
import ch.lueem.tradingbot.core.runtime.MarketSnapshot;
import ch.lueem.tradingbot.core.runtime.RuntimeCycleResult;
import ch.lueem.tradingbot.core.runtime.SequenceMarketSnapshotProvider;
import ch.lueem.tradingbot.core.runtime.TradingDefinition;
import ch.lueem.tradingbot.core.runtime.TradingRuntime;
import ch.lueem.tradingbot.core.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.core.strategy.action.QueuedActionEvaluator;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.junit.jupiter.api.Test;

class PaperBotLoopTest {

    @Test
    void run_executesCyclesThroughRunnerLoop() {
        AtomicInteger executions = new AtomicInteger();
        AtomicLong sleptMillis = new AtomicLong();
        PaperBotLoop loop = new PaperBotLoop(completedCycles -> completedCycles < 2, sleptMillis::set);

        loop.run(session(executions), false);

        assertEquals(2, executions.get());
        assertEquals(250L, sleptMillis.get());
    }

    @Test
    void executionLabel_mapsExecutedToPlaced() {
        assertEquals("PLACED", PaperBotLoop.executionLabel(runtimeCycleResult(Status.EXECUTED, true)));
        assertEquals("VALIDATED", PaperBotLoop.executionLabel(runtimeCycleResult(Status.VALIDATED, true)));
        assertEquals("SKIPPED", PaperBotLoop.executionLabel(runtimeCycleResult(Status.SKIPPED, false)));
    }

    @Test
    void positionLabel_usesPortfolioState() {
        assertEquals("OPEN", PaperBotLoop.positionLabel(runtimeCycleResult(Status.EXECUTED, true)));
        assertEquals("FLAT", PaperBotLoop.positionLabel(runtimeCycleResult(Status.SKIPPED, false)));
    }

    private PaperBotSession session(AtomicInteger executions) {
        TradingRuntime runtime = new TradingRuntime(
                queuedDefinition(),
                new SequenceMarketSnapshotProvider(List.of(
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:15:30Z"), new BigDecimal("100.00"), List.of(new BigDecimal("100.00")), 0),
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:16:30Z"), new BigDecimal("101.00"), List.of(new BigDecimal("100.00"), new BigDecimal("101.00")), 1))),
                new StaticPortfolioService("BTCUSDT", new BigDecimal("1000.0000")),
                new QueuedActionEvaluator(List.of(TradeAction.BUY, TradeAction.SELL)),
                new CountingExecutionService(executions));

        PaperConfig paper = paperConfig();
        return new PaperBotSession(runtime, paper, "https://testnet.binance.vision");
    }

    private RuntimeCycleResult runtimeCycleResult(Status status, boolean openPosition) {
        return new RuntimeCycleResult(
                new MarketSnapshot(
                        "BTCUSDT",
                        "1m",
                        OffsetDateTime.parse("2026-03-12T10:15:30Z"),
                        new BigDecimal("100.00"),
                        List.of(new BigDecimal("100.00")),
                        0),
                new ch.lueem.tradingbot.core.portfolio.PortfolioSnapshot(
                        "BTCUSDT",
                        new BigDecimal("1000.0000"),
                        openPosition
                                ? new ch.lueem.tradingbot.core.portfolio.PositionSnapshot(
                                        true,
                                        new BigDecimal("0.0010"),
                                        new BigDecimal("100.0000"),
                                        OffsetDateTime.parse("2026-03-12T10:14:30Z"))
                                : ch.lueem.tradingbot.core.portfolio.PositionSnapshot.flat()),
                TradeAction.HOLD,
                new Result(status, status != Status.SKIPPED, openPosition, "detail"));
    }

    private static final class CountingExecutionService implements ExecutionService {
        private final AtomicInteger executions;

        private CountingExecutionService(AtomicInteger executions) {
            this.executions = executions;
        }

        @Override
        public Result execute(Request request) {
            executions.incrementAndGet();
            return new Result(Status.VALIDATED, false, false, "validated_only");
        }
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

    private PaperConfig paperConfig() {
        return new PaperConfig(
                new PaperBotConfig("bot-1", "v1", "BTCUSDT", "1m"),
                new PaperExecutionConfig(
                        PaperExchange.BINANCE_SPOT_TESTNET,
                        PaperOrderMode.VALIDATE_ONLY,
                        250L,
                        1000.0,
                        new BigDecimal("0.0010"),
                        false,
                        new BigDecimal("25.0")),
                new PaperStrategyConfig("queued_actions", null, List.of(TradeAction.BUY, TradeAction.SELL)),
                new BinanceConfig("api-key", "secret-key", 15000.0));
    }
}
