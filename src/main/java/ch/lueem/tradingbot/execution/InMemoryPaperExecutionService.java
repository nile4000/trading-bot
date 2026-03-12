package ch.lueem.tradingbot.execution;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ch.lueem.tradingbot.portfolio.InMemoryPortfolioService;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.portfolio.PositionSnapshot;
import ch.lueem.tradingbot.strategy.signal.TradeSignal;

/**
 * Simulates paper-trading executions in memory without talking to a real exchange.
 */
public class InMemoryPaperExecutionService implements ExecutionService {

    private static final int QUANTITY_SCALE = 8;

    private final InMemoryPortfolioService portfolioService;

    public InMemoryPaperExecutionService(InMemoryPortfolioService portfolioService) {
        if (portfolioService == null) {
            throw new IllegalArgumentException("portfolioService must not be null.");
        }
        this.portfolioService = portfolioService;
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null.");
        }

        PortfolioSnapshot snapshot = portfolioService.getSnapshot(request.symbol());
        PositionSnapshot position = snapshot.position();

        return switch (request.tradeSignal()) {
            case HOLD -> new ExecutionResult(false, position.open(), "No execution for HOLD signal.");
            case BUY -> handleBuy(request, snapshot, position);
            case SELL -> handleSell(request, position);
        };
    }

    private ExecutionResult handleBuy(
            ExecutionRequest request,
            PortfolioSnapshot snapshot,
            PositionSnapshot position) {
        if (position.open()) {
            return new ExecutionResult(false, true, "BUY ignored because a position is already open.");
        }

        BigDecimal referencePrice = request.referencePrice();
        if (referencePrice == null || referencePrice.signum() <= 0) {
            return new ExecutionResult(false, false, "BUY ignored because reference price is invalid.");
        }
        if (snapshot.availableCash().signum() <= 0) {
            return new ExecutionResult(false, false, "BUY ignored because no cash is available.");
        }

        BigDecimal quantity = snapshot.availableCash()
                .divide(referencePrice, QUANTITY_SCALE, RoundingMode.HALF_UP);

        portfolioService.openPosition(request.symbol(), quantity, referencePrice, request.requestedAt());
        return new ExecutionResult(true, true, "BUY executed in paper mode.");
    }

    private ExecutionResult handleSell(ExecutionRequest request, PositionSnapshot position) {
        if (!position.open()) {
            return new ExecutionResult(false, false, "SELL ignored because no position is open.");
        }

        BigDecimal referencePrice = request.referencePrice();
        if (referencePrice == null || referencePrice.signum() <= 0) {
            return new ExecutionResult(false, true, "SELL ignored because reference price is invalid.");
        }

        portfolioService.closePosition(request.symbol(), referencePrice);
        return new ExecutionResult(true, false, "SELL executed in paper mode.");
    }
}
