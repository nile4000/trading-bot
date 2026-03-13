package ch.lueem.tradingbot.quarkus;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Runs Quarkus tests with the paper config profile enabled.
 */
public class PaperProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "paper";
    }
}
