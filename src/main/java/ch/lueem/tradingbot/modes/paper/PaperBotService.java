package ch.lueem.tradingbot.modes.paper;

import ch.lueem.tradingbot.adapters.config.ApplicationConfig;
import ch.lueem.tradingbot.adapters.execution.BinanceSpotTestnetClientFactory;

/**
 * Orchestrates one configured paper bot run without owning infrastructure setup
 * or cycle control.
 */
public class PaperBotService {

    private final PaperBotSetup setup;
    private final PaperBotLoop loop;

    public PaperBotService() {
        this(new PaperBotSetup(new BinanceSpotTestnetClientFactory(), System::getenv), new PaperBotLoop());
    }

    public PaperBotService(PaperBotSetup setup, PaperBotLoop loop) {
        this.setup = setup;
        this.loop = loop;
    }

    public void run(ApplicationConfig config) {
        config.paper().validate();
        loop.run(setup.createSession(config.paper()), config.logging());
    }
}
