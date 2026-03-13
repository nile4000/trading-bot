package ch.lueem.tradingbot.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(PaperProfile.class)
class PaperProfileRuntimeConfigTest {

    @Inject
    PaperConfig paperConfig;

    @Inject
    TradingBotRuntimeConfig runtimeConfig;

    @Test
    void appliesPaperProfileOverrides() {
        assertTrue(runtimeConfig.app().lifecycleEvents());
        assertEquals("ema_cross", paperConfig.strategy().name());
        assertEquals("BTCUSDT", paperConfig.bot().symbol());
    }
}
