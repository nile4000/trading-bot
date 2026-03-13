package ch.lueem.tradingbot.modes.paper;

import ch.lueem.tradingbot.adapters.config.paper.PaperConfig;
import ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClientFactory;

/**
 * Orchestrates one configured paper bot run without owning infrastructure setup
 * or cycle control.
 */
public class PaperBotService {

    private final PaperBotSetup setup;
    private final PaperBotLoop loop;

    public PaperBotService() {
        this(new PaperBotSetup(new BinanceClientFactory()), new PaperBotLoop());
    }

    public PaperBotService(PaperBotSetup setup, PaperBotLoop loop) {
        this.setup = setup;
        this.loop = loop;
    }

    public void run(PaperConfig paper, boolean lifecycleEvents) {
        loop.run(setup.createSession(paper), lifecycleEvents);
    }
}
