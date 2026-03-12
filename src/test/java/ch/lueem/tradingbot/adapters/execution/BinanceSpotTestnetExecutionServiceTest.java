package ch.lueem.tradingbot.adapters.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                        PaperOrderMode.VALIDATE_ONLY);

        ExecutionResult result = service.execute(request(TradeAction.BUY));

        assertEquals(ExecutionStatus.VALIDATED, result.status());
        assertTrue(result.executed());
        assertEquals("BTCUSDT", client.lastRequest.getSymbol());
        assertEquals("BUY", client.lastRequest.getSide().getValue());
        assertEquals(0.01d, client.lastRequest.getQuantity());
        assertEquals(15000.0, client.lastRequest.getRecvWindow());
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
                        PaperOrderMode.VALIDATE_ONLY);

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
                        PaperOrderMode.VALIDATE_ONLY);

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> service.execute(request(TradeAction.BUY)));

        assertTrue(exception.getMessage().contains("BUY action"));
        assertTrue(exception.getMessage().contains("BTCUSDT"));
    }

    @Test
    void constructor_rejectsUnsupportedOrderMode() {
        CapturingClient client = new CapturingClient();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new BinanceSpotTestnetExecutionService(
                        client,
                        new PaperPortfolioService("BTCUSDT", new BigDecimal("1000.0000")),
                        new BigDecimal("0.0100"),
                        15000.0,
                        PaperOrderMode.PLACE_ORDER));

        assertTrue(exception.getMessage().contains("VALIDATE_ONLY"));
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
        private boolean throwOnValidate;

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
    }
}
