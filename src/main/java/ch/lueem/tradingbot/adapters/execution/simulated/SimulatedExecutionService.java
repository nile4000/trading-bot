package ch.lueem.tradingbot.adapters.execution.simulated;

import java.math.BigDecimal;

import ch.lueem.tradingbot.adapters.portfolio.SimulatedPortfolioService;
import ch.lueem.tradingbot.core.execution.ExecutionService;
import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.execution.Result;
import ch.lueem.tradingbot.core.execution.Status;
import ch.lueem.tradingbot.core.portfolio.PortfolioSnapshot;
import ch.lueem.tradingbot.core.portfolio.PositionSnapshot;
import org.ta4j.core.analysis.cost.CostModel;
import org.ta4j.core.num.DecimalNum;

/**
 * Simulates fixed-quantity spot executions for backtests without calling an exchange.
 */
public class SimulatedExecutionService implements ExecutionService {

    private final SimulatedPortfolioService portfolioService;
    private final BigDecimal orderQuantity;
    private final BigDecimal slippageRate;
    private final CostModel transactionCostModel;

    public SimulatedExecutionService(
            SimulatedPortfolioService portfolioService,
            BigDecimal orderQuantity,
            BigDecimal slippageRate,
            CostModel transactionCostModel) {
        if (portfolioService == null) {
            throw new IllegalArgumentException("portfolioService must not be null.");
        }
        if (transactionCostModel == null) {
            throw new IllegalArgumentException("transactionCostModel must not be null.");
        }
        if (orderQuantity == null || orderQuantity.signum() <= 0) {
            throw new IllegalArgumentException("orderQuantity must be greater than zero.");
        }
        if (slippageRate == null || slippageRate.signum() < 0 || slippageRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("slippageRate must be between zero (inclusive) and one (exclusive).");
        }
        this.portfolioService = portfolioService;
        this.orderQuantity = orderQuantity;
        this.slippageRate = slippageRate;
        this.transactionCostModel = transactionCostModel;
    }

    @Override
    public Result execute(Request request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null.");
        }

        PortfolioSnapshot snapshot = portfolioService.getSnapshot(request.symbol());
        PositionSnapshot position = snapshot.position();

        return switch (request.tradeAction()) {
            case HOLD -> new Result(Status.SKIPPED, false, position.open(), "No execution for HOLD action.");
            case BUY -> buying(request, snapshot, position);
            case SELL -> selling(request, position);
        };
    }

    private Result buying(
            Request request,
            PortfolioSnapshot snapshot,
            PositionSnapshot position) {
        if (position.open()) {
            return new Result(Status.SKIPPED, false, true, "BUY ignored because a position is already open.");
        }

        BigDecimal referencePrice = request.referencePrice();
        if (referencePrice == null || referencePrice.signum() <= 0) {
            return new Result(Status.SKIPPED, false, false, "BUY ignored because reference price is invalid.");
        }
        BigDecimal fillPrice = buyFillPrice(referencePrice);
        BigDecimal fee = transactionCost(fillPrice, orderQuantity);
        BigDecimal totalCost = fillPrice.multiply(orderQuantity).add(fee);
        if (snapshot.availableCash().compareTo(totalCost) < 0) {
            return new Result(Status.SKIPPED, false, false, "BUY ignored because available cash is insufficient.");
        }

        portfolioService.openPosition(request.symbol(), orderQuantity, fillPrice, fee, request.requestedAt());
        return new Result(Status.EXECUTED, true, true, "BUY executed in simulated mode.");
    }

    private Result selling(Request request, PositionSnapshot position) {
        if (!position.open()) {
            return new Result(Status.SKIPPED, false, false, "SELL ignored because no position is open.");
        }

        BigDecimal referencePrice = request.referencePrice();
        if (referencePrice == null || referencePrice.signum() <= 0) {
            return new Result(Status.SKIPPED, false, true, "SELL ignored because reference price is invalid.");
        }

        BigDecimal fillPrice = sellFillPrice(referencePrice);
        BigDecimal fee = transactionCost(fillPrice, position.quantity());
        portfolioService.closePosition(request.symbol(), fillPrice, fee);
        return new Result(Status.EXECUTED, true, false, "SELL executed in simulated mode.");
    }

    private BigDecimal transactionCost(BigDecimal price, BigDecimal quantity) {
        return transactionCostModel.calculate(
                DecimalNum.valueOf(price),
                DecimalNum.valueOf(quantity))
                .bigDecimalValue();
    }

    private BigDecimal buyFillPrice(BigDecimal referencePrice) {
        return referencePrice.multiply(BigDecimal.ONE.add(slippageRate));
    }

    private BigDecimal sellFillPrice(BigDecimal referencePrice) {
        return referencePrice.multiply(BigDecimal.ONE.subtract(slippageRate));
    }
}
