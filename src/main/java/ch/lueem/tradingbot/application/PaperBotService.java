package ch.lueem.tradingbot.application;

/**
 * Orchestrates one configured paper bot run without owning infrastructure setup or loop control.
 */
public class PaperBotService {

    private final PaperBotBootstrap bootstrap;
    private final PaperBotRunner runner;

    public PaperBotService() {
        this(
                new PaperBotBootstrap(
                        new ch.lueem.tradingbot.integration.binance.spottestnet.BinanceSpotTestnetClientFactory(),
                        System::getenv),
                new PaperBotRunner());
    }

    public PaperBotService(PaperBotBootstrap bootstrap, PaperBotRunner runner) {
        if (bootstrap == null) {
            throw new IllegalArgumentException("bootstrap must not be null.");
        }
        if (runner == null) {
            throw new IllegalArgumentException("runner must not be null.");
        }
        this.bootstrap = bootstrap;
        this.runner = runner;
    }

    public void run(ApplicationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null.");
        }
        if (config.paper() == null) {
            throw new IllegalArgumentException("paper config must not be null.");
        }
        config.paper().validate();

        runner.run(bootstrap.createContext(config.paper()), config.logging());
    }
}
