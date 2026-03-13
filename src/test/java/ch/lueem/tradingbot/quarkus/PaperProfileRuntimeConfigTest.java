package ch.lueem.tradingbot.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.adapters.config.paper.PaperOrderMode;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(PaperProfile.class)
class PaperProfileRuntimeConfigTest {

    @Inject
    PaperConfig paperConfig;

    @Test
    void appliesPaperProfileOverrides() {
        assertEquals(PaperOrderMode.VALIDATE_ONLY, paperConfig.execution().orderMode());
    }
}
