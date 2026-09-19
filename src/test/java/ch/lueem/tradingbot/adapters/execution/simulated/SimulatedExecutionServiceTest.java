package ch.lueem.tradingbot.adapters.execution.simulated;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.adapters.config.backtest.BacktestConfig;
import ch.lueem.tradingbot.adapters.portfolio.SimulatedPortfolioService;
import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.ta4j.core.analysis.cost.LinearTransactionCostModel;
import org.ta4j.core.num.DecimalNum;

@QuarkusTest
class SimulatedExecutionServiceTest {

    @Inject
    BacktestConfig backtestConfig;

    @Test
    void tradesConfiguredFixedQuantityAndAppliesFeesToBothSidesOfRoundTrip() {
        var portfolio = new SimulatedPortfolioService();
        portfolio.seedCash("BTCUSDT", new BigDecimal("1000"));
        var transactionCosts = new LinearTransactionCostModel(
                backtestConfig.executionFeeRate().doubleValue());
        var execution = new SimulatedExecutionService(
                portfolio,
                new BigDecimal("0.0001"),
                backtestConfig.slippageRate(),
                transactionCosts);
        var requestedAt = OffsetDateTime.parse("2026-09-19T10:00:00Z");

        execution.execute(request(TradeAction.BUY, requestedAt));

        BigDecimal quantity = new BigDecimal("0.0001");
        BigDecimal buyFee = transactionCosts.calculate(
                DecimalNum.valueOf(new BigDecimal("100")),
                DecimalNum.valueOf(quantity))
                .bigDecimalValue();
        assertEquals(new BigDecimal("0.00010000"),
                portfolio.getSnapshot("BTCUSDT").position().quantity());
        BigDecimal expectedCashAfterBuy = new BigDecimal("1000")
                .subtract(quantity.multiply(new BigDecimal("100")))
                .subtract(buyFee)
                .setScale(4, java.math.RoundingMode.HALF_UP);
        assertEquals(expectedCashAfterBuy, portfolio.getSnapshot("BTCUSDT").availableCash());

        execution.execute(request(TradeAction.SELL, requestedAt.plusMinutes(1)));

        BigDecimal sellFee = transactionCosts.calculate(
                DecimalNum.valueOf(new BigDecimal("100")),
                DecimalNum.valueOf(quantity))
                .bigDecimalValue();
        BigDecimal expectedCash = expectedCashAfterBuy
                .add(quantity.multiply(new BigDecimal("100")))
                .subtract(sellFee)
                .setScale(4, java.math.RoundingMode.HALF_UP);
        assertEquals(expectedCash,
                portfolio.getSnapshot("BTCUSDT").availableCash());
    }

    @Test
    void appliesFeeAndSlippageOnBothSidesOfRoundTrip() {
        var portfolio = new SimulatedPortfolioService();
        portfolio.seedCash("BTCUSDT", new BigDecimal("10000"));
        var execution = new SimulatedExecutionService(
                portfolio,
                BigDecimal.TEN,
                new BigDecimal("0.0005"),
                new LinearTransactionCostModel(0.001));
        var requestedAt = OffsetDateTime.parse("2026-09-19T10:00:00Z");

        execution.execute(request(TradeAction.BUY, requestedAt));

        assertEquals(new BigDecimal("100.0500"), portfolio.getSnapshot("BTCUSDT").position().entryPrice());

        execution.execute(request(TradeAction.SELL, requestedAt.plusMinutes(1)));

        assertEquals(new BigDecimal("9997.0000"), portfolio.getSnapshot("BTCUSDT").availableCash());
    }

    private Request request(TradeAction action, OffsetDateTime requestedAt) {
        return new Request("backtest", "BTCUSDT", "1m", action, requestedAt, new BigDecimal("100"));
    }
}
