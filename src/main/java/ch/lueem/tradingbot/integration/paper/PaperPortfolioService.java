package ch.lueem.tradingbot.integration.paper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.portfolio.PortfolioService;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.portfolio.PositionSnapshot;

/**
 * Stores local paper portfolio state for one symbol across validated order cycles.
 */
public class PaperPortfolioService implements PortfolioService {

    private static final int MONEY_SCALE = 4;
    private static final int QUANTITY_SCALE = 8;

    private final String symbol;

    private PortfolioSnapshot snapshot;

    public PaperPortfolioService(String symbol, BigDecimal initialCash) {
        validateSymbol(symbol);
        validateNonNegativeAmount(initialCash, "initialCash");
        this.symbol = symbol;
        this.snapshot = new PortfolioSnapshot(
                symbol,
                scaleMoney(initialCash),
                PositionSnapshot.flat());
    }

    @Override
    public synchronized PortfolioSnapshot getSnapshot(String symbol) {
        validateSymbol(symbol);
        ensureKnownSymbol(symbol);
        return snapshot;
    }

    public synchronized void openPosition(
            String symbol,
            BigDecimal quantity,
            BigDecimal entryPrice,
            OffsetDateTime openedAt) {
        validateSymbol(symbol);
        ensureKnownSymbol(symbol);
        validatePositiveAmount(quantity, "quantity");
        validatePositiveAmount(entryPrice, "entryPrice");
        if (openedAt == null) {
            throw new IllegalArgumentException("openedAt must not be null.");
        }

        PositionSnapshot currentPosition = snapshot.position();
        if (currentPosition.open()) {
            throw new IllegalStateException("Cannot open paper position because a position is already open for " + symbol);
        }

        BigDecimal cost = scaleMoney(quantity.multiply(entryPrice));
        if (snapshot.availableCash().compareTo(cost) < 0) {
            throw new IllegalStateException("Cannot open paper position because available cash is insufficient for " + symbol);
        }

        snapshot = new PortfolioSnapshot(
                symbol,
                scaleMoney(snapshot.availableCash().subtract(cost)),
                new PositionSnapshot(true, scaleQuantity(quantity), scaleMoney(entryPrice), openedAt));
    }

    public synchronized void closePosition(String symbol, BigDecimal exitPrice) {
        validateSymbol(symbol);
        ensureKnownSymbol(symbol);
        validatePositiveAmount(exitPrice, "exitPrice");

        PositionSnapshot currentPosition = snapshot.position();
        if (!currentPosition.open()) {
            throw new IllegalStateException("Cannot close paper position because no open position exists for " + symbol);
        }

        BigDecimal proceeds = scaleMoney(currentPosition.quantity().multiply(exitPrice));
        snapshot = new PortfolioSnapshot(
                symbol,
                scaleMoney(snapshot.availableCash().add(proceeds)),
                PositionSnapshot.flat());
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleQuantity(BigDecimal value) {
        return value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private void ensureKnownSymbol(String symbol) {
        if (!this.symbol.equals(symbol)) {
            throw new IllegalArgumentException("Unknown symbol for paper portfolio service: " + symbol);
        }
    }

    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank.");
        }
    }

    private void validateNonNegativeAmount(BigDecimal value, String fieldName) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative.");
        }
    }

    private void validatePositiveAmount(BigDecimal value, String fieldName) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
    }
}
