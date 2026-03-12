package ch.lueem.tradingbot.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores paper-trading portfolio state for local bot runtime and execution flows.
 */
public class PaperPortfolioService implements PortfolioService {

    private static final int MONEY_SCALE = 4;
    private static final int QUANTITY_SCALE = 8;

    private final Map<String, PortfolioSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public PortfolioSnapshot getSnapshot(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank.");
        }

        return snapshots.getOrDefault(symbol, new PortfolioSnapshot(symbol, BigDecimal.ZERO.setScale(MONEY_SCALE), PositionSnapshot.flat()));
    }

    public void seedCash(String symbol, BigDecimal availableCash) {
        validateSymbol(symbol);
        validateAmount(availableCash, "availableCash");

        snapshots.put(symbol, new PortfolioSnapshot(symbol, scaleMoney(availableCash), PositionSnapshot.flat()));
    }

    public void openPosition(String symbol, BigDecimal quantity, BigDecimal entryPrice, OffsetDateTime openedAt) {
        validateSymbol(symbol);
        validateAmount(quantity, "quantity");
        validateAmount(entryPrice, "entryPrice");
        if (openedAt == null) {
            throw new IllegalArgumentException("openedAt must not be null.");
        }

        snapshots.put(symbol, new PortfolioSnapshot(
                symbol,
                BigDecimal.ZERO.setScale(MONEY_SCALE),
                new PositionSnapshot(true, scaleQuantity(quantity), scaleMoney(entryPrice), openedAt)));
    }

    public void closePosition(String symbol, BigDecimal exitPrice) {
        validateSymbol(symbol);
        validateAmount(exitPrice, "exitPrice");

        PortfolioSnapshot currentSnapshot = getSnapshot(symbol);
        PositionSnapshot position = currentSnapshot.position();
        if (!position.open()) {
            throw new IllegalStateException("Cannot close position because no open position exists for " + symbol);
        }

        BigDecimal cashAfterClose = scaleMoney(position.quantity().multiply(exitPrice));
        snapshots.put(symbol, new PortfolioSnapshot(symbol, cashAfterClose, PositionSnapshot.flat()));
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleQuantity(BigDecimal value) {
        return value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank.");
        }
    }

    private void validateAmount(BigDecimal value, String fieldName) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
    }
}
