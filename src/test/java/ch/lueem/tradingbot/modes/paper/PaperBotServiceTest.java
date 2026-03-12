package ch.lueem.tradingbot.modes.paper;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.util.List;

import ch.lueem.tradingbot.adapters.config.ApplicationConfig;
import ch.lueem.tradingbot.adapters.config.ApplicationMode;
import ch.lueem.tradingbot.adapters.config.BinanceSpotTestnetConfig;
import ch.lueem.tradingbot.adapters.config.LoggingConfig;
import ch.lueem.tradingbot.adapters.config.PaperBotConfig;
import ch.lueem.tradingbot.adapters.config.PaperConfig;
import ch.lueem.tradingbot.adapters.config.PaperExchange;
import ch.lueem.tradingbot.adapters.config.PaperExecutionConfig;
import ch.lueem.tradingbot.adapters.config.PaperOrderMode;
import ch.lueem.tradingbot.adapters.config.PaperStrategyConfig;
import ch.lueem.tradingbot.adapters.config.ReportingConfig;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import org.junit.jupiter.api.Test;

class PaperBotServiceTest {

    @Test
    void run_delegatesToSetupAndLoop() {
        RecordingSetup setup = new RecordingSetup();
        RecordingLoop loop = new RecordingLoop();
        PaperBotService service = new PaperBotService(setup, loop);
        ApplicationConfig config = config(PaperOrderMode.VALIDATE_ONLY);

        service.run(config);

        assertSame(config.paper(), setup.paper);
        assertSame(setup.session, loop.session);
        assertSame(config.logging(), loop.logging);
    }

    private ApplicationConfig config(PaperOrderMode orderMode) {
        return new ApplicationConfig(
                ApplicationMode.PAPER,
                null,
                new PaperConfig(
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
                        new BinanceSpotTestnetConfig("BINANCE_TESTNET_API_KEY", "BINANCE_TESTNET_SECRET_KEY", 15000.0)),
                new ReportingConfig(true, true),
                new LoggingConfig(false));
    }

    private static final class RecordingSetup extends PaperBotSetup {
        private PaperConfig paper;
        private final PaperBotSession session =
                new PaperBotSession(null, null, "https://testnet.binance.vision");

        private RecordingSetup() {
            super(new ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClientFactory(), name -> "");
        }

        @Override
        public PaperBotSession createSession(PaperConfig paper) {
            this.paper = paper;
            return session;
        }
    }

    private static final class RecordingLoop extends PaperBotLoop {
        private PaperBotSession session;
        private LoggingConfig logging;

        private RecordingLoop() {
            super(completedCycles -> false, millis -> {
            });
        }

        @Override
        public void run(PaperBotSession session, LoggingConfig logging) {
            this.session = session;
            this.logging = logging;
        }
    }
}
