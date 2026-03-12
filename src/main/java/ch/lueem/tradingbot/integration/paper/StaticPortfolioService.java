package ch.lueem.tradingbot.integration.paper;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ch.lueem.tradingbot.portfolio.PortfolioService;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.portfolio.PositionSnapshot;

/**
 * Returns a stable, non-mutating portfolio snapshot for validate-only exchange flows.
 */
public class StaticPortfolioService implements PortfolioService {

    private static final int MONEY_SCALE = 4;

    private final PortfolioSnapshot snapshot;

    public StaticPortfolioService(String symbol, BigDecimal availableCash) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank.");
        }
        if (availableCash == null || availableCash.signum() < 0) {
            throw new IllegalArgumentException("availableCash must not be negative.");
        }
        this.snapshot = new PortfolioSnapshot(
                symbol,
                availableCash.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                PositionSnapshot.flat());
    }

    @Override
    public PortfolioSnapshot getSnapshot(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank.");
        }
        if (!snapshot.symbol().equals(symbol)) {
            throw new IllegalArgumentException("Unknown symbol for static portfolio service: " + symbol);
        }
        return snapshot;
    }
}
