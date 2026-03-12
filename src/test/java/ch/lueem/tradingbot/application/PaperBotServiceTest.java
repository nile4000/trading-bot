package ch.lueem.tradingbot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.util.List;

import ch.lueem.tradingbot.strategy.signal.TradeSignal;
import org.junit.jupiter.api.Test;

class PaperBotServiceTest {

    @Test
    void run_delegatesToBootstrapAndRunner() {
        RecordingBootstrap bootstrap = new RecordingBootstrap();
        RecordingRunner runner = new RecordingRunner();
        PaperBotService service = new PaperBotService(bootstrap, runner);
        ApplicationConfig config = config(PaperOrderMode.VALIDATE_ONLY);

        service.run(config);

        assertSame(config.paper(), bootstrap.paper);
        assertSame(bootstrap.context, runner.context);
        assertSame(config.logging(), runner.logging);
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
                                new BigDecimal("0.0010")),
                        new PaperSignalSourceConfig("queued_signals", List.of(TradeSignal.BUY, TradeSignal.SELL)),
                        new BinanceSpotTestnetConfig("BINANCE_TESTNET_API_KEY", "BINANCE_TESTNET_SECRET_KEY", 15000.0)),
                new ReportingConfig(true, true),
                new LoggingConfig(false));
    }

    private static final class RecordingBootstrap extends PaperBotBootstrap {
        private PaperConfig paper;
        private final PaperBotRuntimeContext context =
                new PaperBotRuntimeContext(null, null, "https://testnet.binance.vision");

        private RecordingBootstrap() {
            super(new ch.lueem.tradingbot.integration.binance.spottestnet.BinanceSpotTestnetClientFactory(), name -> "");
        }

        @Override
        public PaperBotRuntimeContext createContext(PaperConfig paper) {
            this.paper = paper;
            return context;
        }
    }

    private static final class RecordingRunner extends PaperBotRunner {
        private PaperBotRuntimeContext context;
        private LoggingConfig logging;

        private RecordingRunner() {
            super(completedCycles -> false, millis -> {
            });
        }

        @Override
        public void run(PaperBotRuntimeContext context, LoggingConfig logging) {
            this.context = context;
            this.logging = logging;
        }
    }
}
