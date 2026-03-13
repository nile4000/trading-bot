package ch.lueem.tradingbot.modes.paper;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.paper.BinanceConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperBotConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperExchange;
import ch.lueem.tradingbot.adapters.config.paper.PaperExecutionConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperOrderMode;
import ch.lueem.tradingbot.adapters.config.paper.PaperStrategyConfig;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.junit.jupiter.api.Test;

class PaperBotServiceTest {

    @Test
    void run_delegatesToSetupAndLoop() {
        RecordingSetup setup = new RecordingSetup();
        RecordingLoop loop = new RecordingLoop();
        PaperBotService service = new PaperBotService(setup, loop);
        PaperConfig paper = config(PaperOrderMode.VALIDATE_ONLY);

        service.run(paper, false);

        assertSame(paper, setup.paper);
        assertSame(setup.session, loop.session);
        assertSame(false, loop.lifecycleEvents);
    }

    private PaperConfig config(PaperOrderMode orderMode) {
        return new PaperConfig(
                new PaperBotConfig("bot-1", "v1", "BTCUSDT", "1m"),
                new PaperExecutionConfig(
                        PaperExchange.BINANCE_SPOT_TESTNET,
                        orderMode,
                        1000L,
                        1000.0,
                        new BigDecimal("0.0010"),
                        false,
                        new BigDecimal("25.0")),
                new PaperStrategyConfig("queued_actions", null, List.of(TradeAction.BUY, TradeAction.SELL)),
                new BinanceConfig("api-key", "secret-key", 15000.0));
    }

    private static final class RecordingSetup extends PaperBotSetup {
        private PaperConfig paper;
        private final PaperBotSession session =
                new PaperBotSession(null, null, "https://testnet.binance.vision");

        private RecordingSetup() {
            super(new ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClientFactory());
        }

        @Override
        public PaperBotSession createSession(PaperConfig paper) {
            this.paper = paper;
            return session;
        }
    }

    private static final class RecordingLoop extends PaperBotLoop {
        private PaperBotSession session;
        private boolean lifecycleEvents;

        private RecordingLoop() {
            super(completedCycles -> false, millis -> {
            });
        }

        @Override
        public void run(PaperBotSession session, boolean lifecycleEvents) {
            this.session = session;
            this.lifecycleEvents = lifecycleEvents;
        }
    }
}
