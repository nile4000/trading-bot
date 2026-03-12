package ch.lueem.tradingbot.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void resolveConfigResource_prefersExplicitArgument() {
        String resource = App.resolveConfigResource(
                new String[] { "application-paper.yml" },
                "application-backtest.yml",
                "application.yml");

        assertEquals("application-paper.yml", resource);
    }

    @Test
    void resolveConfigResource_prefersSystemPropertyOverEnv() {
        String resource = App.resolveConfigResource(
                new String[0],
                "application-paper.yml",
                "application-backtest.yml");

        assertEquals("application-paper.yml", resource);
    }

    @Test
    void resolveConfigResource_fallsBackToDefault() {
        String resource = App.resolveConfigResource(
                new String[0],
                null,
                null);

        assertEquals(App.DEFAULT_CONFIG, resource);
    }
}
