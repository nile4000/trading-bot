package ch.lueem.tradingbot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ch.lueem.tradingbot.bot.market.MarketSnapshot;
import ch.lueem.tradingbot.bot.market.SequenceMarketSnapshotProvider;
import ch.lueem.tradingbot.bot.model.BotMode;
import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.execution.ExecutionService;
import ch.lueem.tradingbot.execution.ExecutionStatus;
import ch.lueem.tradingbot.integration.paper.StaticPortfolioService;
import ch.lueem.tradingbot.runtime.TradingDefinition;
import ch.lueem.tradingbot.runtime.TradingRuntime;
import ch.lueem.tradingbot.strategy.definition.StrategyDefinition;
import ch.lueem.tradingbot.strategy.signal.QueuedSignalEvaluator;
import ch.lueem.tradingbot.strategy.signal.TradeSignal;
import org.junit.jupiter.api.Test;

class PaperBotRunnerTest {

    @Test
    void run_executesCyclesThroughRunnerLoop() {
        AtomicInteger executions = new AtomicInteger();
        AtomicLong sleptMillis = new AtomicLong();
        PaperBotRunner runner = new PaperBotRunner(completedCycles -> completedCycles < 2, sleptMillis::set);

        runner.run(context(executions), new LoggingConfig(false));

        assertEquals(2, executions.get());
        assertEquals(250L, sleptMillis.get());
    }

    private PaperBotRuntimeContext context(AtomicInteger executions) {
        TradingRuntime runtime = new TradingRuntime(
                new TradingDefinition("bot-1", "v1", BotMode.PAPER, "BTCUSDT", "1m", new StrategyDefinition("queued_signals", null)),
                new SequenceMarketSnapshotProvider(List.of(
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:15:30Z"), new BigDecimal("100.00"), List.of(new BigDecimal("100.00"))),
                        new MarketSnapshot("BTCUSDT", "1m", OffsetDateTime.parse("2026-03-12T10:16:30Z"), new BigDecimal("101.00"), List.of(new BigDecimal("100.00"), new BigDecimal("101.00"))))),
                new StaticPortfolioService("BTCUSDT", new BigDecimal("1000.0000")),
                new QueuedSignalEvaluator(List.of(TradeSignal.BUY, TradeSignal.SELL)),
                new CountingExecutionService(executions));

        PaperConfig paper = new PaperConfig(
                new PaperBotConfig("bot-1", "v1", "BTCUSDT", "1m"),
                new PaperExecutionConfig(
                        PaperExchange.BINANCE_SPOT_TESTNET,
                        PaperOrderMode.VALIDATE_ONLY,
                        250L,
                        1000.0,
                        new BigDecimal("0.0010")),
                new PaperSignalSourceConfig("queued_signals", List.of(TradeSignal.BUY, TradeSignal.SELL)),
                new BinanceSpotTestnetConfig("BINANCE_TESTNET_API_KEY", "BINANCE_TESTNET_SECRET_KEY", 15000.0));
        return new PaperBotRuntimeContext(runtime, paper, "https://testnet.binance.vision");
    }

    private static final class CountingExecutionService implements ExecutionService {
        private final AtomicInteger executions;

        private CountingExecutionService(AtomicInteger executions) {
            this.executions = executions;
        }

        @Override
        public ExecutionResult execute(ch.lueem.tradingbot.execution.ExecutionRequest request) {
            executions.incrementAndGet();
            return new ExecutionResult(ExecutionStatus.VALIDATED, false, false, "validated only");
        }
    }
}
