package ch.lueem.tradingbot.quarkus;

import io.quarkus.test.junit.QuarkusTestProfile;

public class PaperProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "paper";
    }
}
