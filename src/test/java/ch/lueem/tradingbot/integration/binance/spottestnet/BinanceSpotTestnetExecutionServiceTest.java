package ch.lueem.tradingbot.integration.binance.spottestnet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ch.lueem.tradingbot.application.PaperOrderMode;
import ch.lueem.tradingbot.execution.ExecutionRequest;
import ch.lueem.tradingbot.execution.ExecutionResult;
import ch.lueem.tradingbot.execution.ExecutionStatus;
import ch.lueem.tradingbot.strategy.signal.TradeSignal;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;
import org.junit.jupiter.api.Test;

class BinanceSpotTestnetExecutionServiceTest {

    @Test
    void execute_mapsBuyToOrderTestRequest() {
        CapturingClient client = new CapturingClient();
        BinanceSpotTestnetExecutionService service =
                new BinanceSpotTestnetExecutionService(
                        client,
                        new BigDecimal("0.0100"),
                        15000.0,
                        PaperOrderMode.VALIDATE_ONLY);

        ExecutionResult result = service.execute(request(TradeSignal.BUY));

        assertEquals(ExecutionStatus.VALIDATED, result.status());
        assertFalse(result.executed());
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
                        new BigDecimal("0.0200"),
                        15000.0,
                        PaperOrderMode.VALIDATE_ONLY);

        ExecutionResult result = service.execute(request(TradeSignal.SELL));

        assertEquals(ExecutionStatus.VALIDATED, result.status());
        assertFalse(result.executed());
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
                        new BigDecimal("0.0100"),
                        15000.0,
                        PaperOrderMode.VALIDATE_ONLY);

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> service.execute(request(TradeSignal.BUY)));

        assertTrue(exception.getMessage().contains("BUY order"));
        assertTrue(exception.getMessage().contains("BTCUSDT"));
    }

    @Test
    void constructor_rejectsUnsupportedOrderMode() {
        CapturingClient client = new CapturingClient();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new BinanceSpotTestnetExecutionService(
                        client,
                        new BigDecimal("0.0100"),
                        15000.0,
                        PaperOrderMode.PLACE_ORDER));

        assertTrue(exception.getMessage().contains("VALIDATE_ONLY"));
    }

    private ExecutionRequest request(TradeSignal signal) {
        return new ExecutionRequest(
                "runtime-1",
                "BTCUSDT",
                "1m",
                signal,
                OffsetDateTime.parse("2026-03-12T10:15:30Z"),
                new BigDecimal("80000.00"));
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
