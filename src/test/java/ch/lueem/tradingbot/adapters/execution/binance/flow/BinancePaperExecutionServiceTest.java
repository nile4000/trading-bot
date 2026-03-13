package ch.lueem.tradingbot.adapters.execution.binance.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.adapters.config.paper.PaperOrderMode;
import ch.lueem.tradingbot.adapters.execution.binance.client.BinanceClient;
import ch.lueem.tradingbot.adapters.portfolio.PaperPortfolioService;
import ch.lueem.tradingbot.core.execution.Request;
import ch.lueem.tradingbot.core.execution.Result;
import ch.lueem.tradingbot.core.execution.Status;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.NewOrderResponse;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;
import org.junit.jupiter.api.Test;

class BinancePaperExecutionServiceTest {

    @Test
    void validatesBuyOrdersWithoutPlacingThem() {
        CapturingClient client = new CapturingClient();
        PaperPortfolioService portfolioService = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.00"));
        BinancePaperExecutionService service =
                new BinancePaperExecutionService(
                        client,
                        portfolioService,
                        new BigDecimal("0.01000000"),
                        5_000.0,
                        PaperOrderMode.VALIDATE_ONLY,
                        true,
                        null);

        Result result = service.execute(buyRequest(new BigDecimal("50000.00")));

        assertEquals(Status.VALIDATED, result.status());
        assertTrue(result.executed());
        assertTrue(result.positionOpenAfterExecution());
        assertEquals("validated_only", result.message());
        assertEquals("BTCUSDT", client.validatedRequest.getSymbol());
        assertEquals(new BigDecimal("0.01000000"), portfolioService.getSnapshot("BTCUSDT").position().quantity());
        assertFalse(client.placeOrderCalled);
    }

    @Test
    void placesSellOrdersWhenConfiguredToDoSo() {
        CapturingClient client = new CapturingClient();
        client.orderResponse = new NewOrderResponse().orderId(42L);
        PaperPortfolioService portfolioService = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.00"));
        portfolioService.openPosition(
                "BTCUSDT",
                new BigDecimal("0.01000000"),
                new BigDecimal("50000.00"),
                OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        BinancePaperExecutionService service =
                new BinancePaperExecutionService(
                        client,
                        portfolioService,
                        new BigDecimal("0.01000000"),
                        5_000.0,
                        PaperOrderMode.PLACE_ORDER,
                        true,
                        null);

        Result result = service.execute(sellRequest(new BigDecimal("51000.00")));

        assertEquals(Status.EXECUTED, result.status());
        assertTrue(result.executed());
        assertFalse(result.positionOpenAfterExecution());
        assertEquals("testnet_order#42", result.message());
        assertEquals("BTCUSDT", client.placedRequest.getSymbol());
        assertFalse(portfolioService.getSnapshot("BTCUSDT").position().open());
    }

    @Test
    void skipsBuyWhenPlaceOrdersAreDisabled() {
        CapturingClient client = new CapturingClient();
        PaperPortfolioService portfolioService = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.00"));
        BinancePaperExecutionService service =
                new BinancePaperExecutionService(
                        client,
                        portfolioService,
                        new BigDecimal("0.01000000"),
                        5_000.0,
                        PaperOrderMode.PLACE_ORDER,
                        false,
                        null);

        Result result = service.execute(buyRequest(new BigDecimal("50000.00")));

        assertEquals(Status.SKIPPED, result.status());
        assertFalse(result.executed());
        assertEquals("place_orders_disabled", result.message());
        assertFalse(client.placeOrderCalled);
    }

    @Test
    void skipsBuyWhenMaxOrderNotionalWouldBeExceeded() {
        CapturingClient client = new CapturingClient();
        PaperPortfolioService portfolioService = new PaperPortfolioService("BTCUSDT", new BigDecimal("10000.00"));
        BinancePaperExecutionService service =
                new BinancePaperExecutionService(
                        client,
                        portfolioService,
                        new BigDecimal("0.02000000"),
                        5_000.0,
                        PaperOrderMode.VALIDATE_ONLY,
                        true,
                        new BigDecimal("500.00"));

        Result result = service.execute(buyRequest(new BigDecimal("30000.00")));

        assertEquals(Status.SKIPPED, result.status());
        assertFalse(result.executed());
        assertEquals("max_notional_exceeded", result.message());
        assertFalse(client.validateCalled);
    }

    @Test
    void wrapsClientFailuresWithContext() {
        CapturingClient client = new CapturingClient();
        client.validationFailure = new IllegalStateException("boom");
        PaperPortfolioService portfolioService = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.00"));
        BinancePaperExecutionService service =
                new BinancePaperExecutionService(
                        client,
                        portfolioService,
                        new BigDecimal("0.01000000"),
                        5_000.0,
                        PaperOrderMode.VALIDATE_ONLY,
                        true,
                        null);

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> service.execute(buyRequest(new BigDecimal("50000.00"))));

        assertTrue(exception.getMessage().contains("validate BUY action on Binance Spot Testnet for symbol BTCUSDT"));
        assertEquals("boom", exception.getCause().getMessage());
    }

    @Test
    void skipsSellWhenNoPositionIsOpen() {
        CapturingClient client = new CapturingClient();
        PaperPortfolioService portfolioService = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.00"));
        BinancePaperExecutionService service =
                new BinancePaperExecutionService(
                        client,
                        portfolioService,
                        new BigDecimal("0.01000000"),
                        5_000.0,
                        PaperOrderMode.VALIDATE_ONLY,
                        true,
                        null);

        Result result = service.execute(sellRequest(new BigDecimal("50000.00")));

        assertEquals(Status.SKIPPED, result.status());
        assertFalse(result.executed());
        assertEquals("no_open_position", result.message());
    }

    private static Request buyRequest(BigDecimal referencePrice) {
        return new Request(
                "runtime-1",
                "BTCUSDT",
                "1m",
                TradeAction.BUY,
                OffsetDateTime.parse("2026-03-13T10:15:00Z"),
                referencePrice);
    }

    private static Request sellRequest(BigDecimal referencePrice) {
        return new Request(
                "runtime-1",
                "BTCUSDT",
                "1m",
                TradeAction.SELL,
                OffsetDateTime.parse("2026-03-13T10:15:00Z"),
                referencePrice);
    }

    private static final class CapturingClient implements BinanceClient {
        private OrderTestRequest validatedRequest;
        private NewOrderRequest placedRequest;
        private boolean validateCalled;
        private boolean placeOrderCalled;
        private RuntimeException validationFailure;
        private NewOrderResponse orderResponse = new NewOrderResponse();

        @Override
        public String baseUrl() {
            return "https://testnet.binance.vision";
        }

        @Override
        public void validateOrder(OrderTestRequest request) {
            validateCalled = true;
            validatedRequest = request;
            if (validationFailure != null) {
                throw validationFailure;
            }
        }

        @Override
        public NewOrderResponse placeOrder(NewOrderRequest request) {
            placeOrderCalled = true;
            placedRequest = request;
            return orderResponse;
        }

        @Override
        public BigDecimal loadSymbolPrice(String symbol) {
            return new BigDecimal("50000.00");
        }
    }
}
