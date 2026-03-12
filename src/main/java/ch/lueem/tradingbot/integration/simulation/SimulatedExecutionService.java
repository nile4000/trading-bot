package ch.lueem.tradingbot.integration.simulation;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ch.lueem.tradingbot.execution.ExecutionRequest;
import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.execution.ExecutionService;
import ch.lueem.tradingbot.execution.ExecutionStatus;
import ch.lueem.tradingbot.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.portfolio.PositionSnapshot;

/**
 * Simulates all-in spot executions without talking to a real exchange.
 */
public class SimulatedExecutionService implements ExecutionService {

    private static final int QUANTITY_SCALE = 8;

    private final SimulatedPortfolioService portfolioService;

    public SimulatedExecutionService(SimulatedPortfolioService portfolioService) {
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
            case HOLD -> new ExecutionResult(ExecutionStatus.SKIPPED, false, position.open(), "No execution for HOLD signal.");
            case BUY -> handleBuy(request, snapshot, position);
            case SELL -> handleSell(request, position);
        };
    }

    private ExecutionResult handleBuy(
            ExecutionRequest request,
            PortfolioSnapshot snapshot,
            PositionSnapshot position) {
        if (position.open()) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, true, "BUY ignored because a position is already open.");
        }

        BigDecimal referencePrice = request.referencePrice();
        if (referencePrice == null || referencePrice.signum() <= 0) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "BUY ignored because reference price is invalid.");
        }
        if (snapshot.availableCash().signum() <= 0) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "BUY ignored because no cash is available.");
        }

        BigDecimal quantity = snapshot.availableCash()
                .divide(referencePrice, QUANTITY_SCALE, RoundingMode.HALF_UP);

        portfolioService.openPosition(request.symbol(), quantity, referencePrice, request.requestedAt());
        return new ExecutionResult(ExecutionStatus.EXECUTED, true, true, "BUY executed in simulated mode.");
    }

    private ExecutionResult handleSell(ExecutionRequest request, PositionSnapshot position) {
        if (!position.open()) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, false, "SELL ignored because no position is open.");
        }

        BigDecimal referencePrice = request.referencePrice();
        if (referencePrice == null || referencePrice.signum() <= 0) {
            return new ExecutionResult(ExecutionStatus.SKIPPED, false, true, "SELL ignored because reference price is invalid.");
        }

        portfolioService.closePosition(request.symbol(), referencePrice);
        return new ExecutionResult(ExecutionStatus.EXECUTED, true, false, "SELL executed in simulated mode.");
    }
}
