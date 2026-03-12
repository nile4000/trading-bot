package ch.lueem.tradingbot.adapters.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.adapters.config.PaperOrderMode;
import ch.lueem.tradingbot.core.execution.ExecutionRequest;
import ch.lueem.tradingbot.core.execution.ExecutionResult;
import ch.lueem.tradingbot.core.execution.ExecutionStatus;
import ch.lueem.tradingbot.adapters.portfolio.PaperPortfolioService;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.NewOrderResponse;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;
import org.junit.jupiter.api.Test;

class BinanceSpotTestnetExecutionServiceTest {

    @Test
    void execute_mapsBuyToOrderTestRequest() {
        CapturingClient client = new CapturingClient();
        BinanceSpotTestnetExecutionService service =
                new BinanceSpotTestnetExecutionService(
                        client,
                        new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.0000")),
                        new BigDecimal("0.0100"),
                        15000.0,
                        PaperOrderMode.VALIDATE_ONLY,
                        false,
                        new BigDecimal("1000.00"));

        ExecutionResult result = service.execute(request(TradeAction.BUY));

        assertEquals(ExecutionStatus.VALIDATED, result.status());
        assertTrue(result.executed());
        assertEquals("BTCUSDT", client.lastRequest.getSymbol());
        assertEquals("BUY", client.lastRequest.getSide().getValue());
        assertEquals(0.01d, client.lastRequest.getQuantity());
        assertEquals(15000.0, client.lastRequest.getRecvWindow());
        assertFalse(client.placeOrderCalled);
    }

    @Test
    void execute_mapsSellToOrderTestRequest() {
        CapturingClient client = new CapturingClient();
        BinanceSpotTestnetExecutionService service =
                new BinanceSpotTestnetExecutionService(
                        client,
                        paperPortfolioWithOpenPosition(),
                        new BigDecimal("0.0200"),
                        15000.0,
                        PaperOrderMode.VALIDATE_ONLY,
                        false,
                        new BigDecimal("1000.00"));

        ExecutionResult result = service.execute(request(TradeAction.SELL));

        assertEquals(ExecutionStatus.VALIDATED, result.status());
        assertTrue(result.executed());
        assertEquals("SELL", client.lastRequest.getSide().getValue());
        assertEquals(0.02d, client.lastRequest.getQuantity());
    }

    @Test
    void execute_wrapsBinanceFailuresWithContext() {
        CapturingClient client = new CapturingClient();
        client.throwOnValidate = true;
        BinanceSpotTestnetExecutionService service =
                new BinanceSpotTestnetExecutionService(
                        client,
                        new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.0000")),
                        new BigDecimal("0.0100"),
                        15000.0,
                        PaperOrderMode.VALIDATE_ONLY,
                        false,
                        new BigDecimal("1000.00"));

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> service.execute(request(TradeAction.BUY)));

        assertTrue(exception.getMessage().contains("BUY action"));
        assertTrue(exception.getMessage().contains("BTCUSDT"));
    }

    @Test
    void execute_placesRealTestnetOrderWhenConfigured() {
        CapturingClient client = new CapturingClient();
        PaperPortfolioService portfolioService = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.0000"));
        BinanceSpotTestnetExecutionService service =
                new BinanceSpotTestnetExecutionService(
                        client,
                        portfolioService,
                        new BigDecimal("0.0100"),
                        15000.0,
                        PaperOrderMode.PLACE_ORDER,
                        true,
                        new BigDecimal("1000.00"));

        ExecutionResult result = service.execute(request(TradeAction.BUY));

        assertEquals(ExecutionStatus.EXECUTED, result.status());
        assertTrue(result.executed());
        assertTrue(client.placeOrderCalled);
        assertEquals("BTCUSDT", client.lastPlacedOrder.getSymbol());
        assertEquals("BUY", client.lastPlacedOrder.getSide().getValue());
        assertEquals("testnet_order#12345", result.message());
        assertTrue(portfolioService.getSnapshot("BTCUSDT").position().open());
    }

    @Test
    void execute_doesNotChangePortfolioWhenPlaceOrderFails() {
        CapturingClient client = new CapturingClient();
        client.throwOnPlace = true;
        PaperPortfolioService portfolioService = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.0000"));
        BinanceSpotTestnetExecutionService service =
                new BinanceSpotTestnetExecutionService(
                        client,
                        portfolioService,
                        new BigDecimal("0.0100"),
                        15000.0,
                        PaperOrderMode.PLACE_ORDER,
                        true,
                        new BigDecimal("1000.00"));

        assertThrows(IllegalStateException.class, () -> service.execute(request(TradeAction.BUY)));
        assertFalse(portfolioService.getSnapshot("BTCUSDT").position().open());
    }

    @Test
    void execute_skipsPlaceOrderWhenDisabled() {
        CapturingClient client = new CapturingClient();
        BinanceSpotTestnetExecutionService service =
                new BinanceSpotTestnetExecutionService(
                        client,
                        new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.0000")),
                        new BigDecimal("0.0100"),
                        15000.0,
                        PaperOrderMode.PLACE_ORDER,
                        false,
                        new BigDecimal("1000.00"));

        ExecutionResult result = service.execute(request(TradeAction.BUY));

        assertEquals(ExecutionStatus.SKIPPED, result.status());
        assertEquals("place_orders_disabled", result.message());
        assertFalse(client.placeOrderCalled);
    }

    @Test
    void execute_skipsBuyAboveMaxOrderNotional() {
        CapturingClient client = new CapturingClient();
        BinanceSpotTestnetExecutionService service =
                new BinanceSpotTestnetExecutionService(
                        client,
                        new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.0000")),
                        new BigDecimal("0.0100"),
                        15000.0,
                        PaperOrderMode.VALIDATE_ONLY,
                        false,
                        new BigDecimal("500.00"));

        ExecutionResult result = service.execute(request(TradeAction.BUY));

        assertEquals(ExecutionStatus.SKIPPED, result.status());
        assertEquals("max_notional_exceeded", result.message());
    }

    private ExecutionRequest request(TradeAction action) {
        return new ExecutionRequest(
                "runtime-1",
                "BTCUSDT",
                "1m",
                action,
                OffsetDateTime.parse("2026-03-12T10:15:30Z"),
                new BigDecimal("80000.00"));
    }

    private PaperPortfolioService paperPortfolioWithOpenPosition() {
        PaperPortfolioService portfolioService = new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.0000"));
        portfolioService.openPosition(
                "BTCUSDT",
                new BigDecimal("0.02000000"),
                new BigDecimal("40000.0000"),
                OffsetDateTime.parse("2026-03-12T10:14:30Z"));
        return portfolioService;
    }

    private static final class CapturingClient implements BinanceSpotTestnetClient {
        private OrderTestRequest lastRequest;
        private NewOrderRequest lastPlacedOrder;
        private boolean throwOnValidate;
        private boolean throwOnPlace;
        private boolean placeOrderCalled;

        @Override
        public String baseUrl() {
            return BinanceSpotTestnetClientFactory.TESTNET_REST_BASE_URL;
        }

        @Override
        public BigDecimal loadSymbolPrice(String symbol) {
            return new BigDecimal("80000.00");
        }

        @Override
        public void validateOrder(OrderTestRequest request) {
            lastRequest = request;
            if (throwOnValidate) {
                throw new IllegalStateException("boom");
            }
        }

        @Override
        public NewOrderResponse placeOrder(NewOrderRequest request) {
            placeOrderCalled = true;
            lastPlacedOrder = request;
            if (throwOnPlace) {
                throw new IllegalStateException("boom");
            }
            return new NewOrderResponse().orderId(12345L).clientOrderId("paper-test");
        }
    }
}
