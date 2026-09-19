package ch.lueem.tradingbot.adapters.binance.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceBalance;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceFill;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceLotSize;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceOrder;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceSymbolInfo;
import ch.lueem.tradingbot.adapters.market.BinanceKline;
import ch.lueem.tradingbot.core.strategy.action.TradeAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.binance.connector.client.common.ApiException;
import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.spot.rest.api.SpotRestApi;
import com.binance.connector.client.spot.rest.model.NewOrderRequest;
import com.binance.connector.client.spot.rest.model.NewOrderResponse;
import com.binance.connector.client.spot.rest.model.OrderTestRequest;
import com.binance.connector.client.spot.rest.model.Interval;
import com.binance.connector.client.spot.rest.model.ExchangeInfoResponseSymbolsInner;
import com.binance.connector.client.spot.rest.model.LotSizeFilter;

/**
 * Bridges the generated Binance Spot REST client to the paper bot abstractions.
 */
public class BinanceSpotRestClient implements BinanceClient {

    private static final int ORDER_NOT_FOUND_CODE = -2013;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ClientConfiguration clientConfiguration;
    private final SpotRestApi spotRestApi;

    public BinanceSpotRestClient(ClientConfiguration clientConfiguration) {
        if (clientConfiguration == null) {
            throw new IllegalArgumentException("clientConfiguration must not be null.");
        }
        this.clientConfiguration = clientConfiguration;
        this.spotRestApi = new SpotRestApi(clientConfiguration);
    }

    @Override
    public String baseUrl() {
        return clientConfiguration.getUrl();
    }

    @Override
    public BinanceSymbolInfo loadSymbolInfo(String symbol) {
        var response = callApi(
                () -> spotRestApi.exchangeInfo(symbol, null, null, null, null).getData(),
                "Failed to load Binance symbol information for " + symbol);
        return response.getSymbols().stream()
                .filter(candidate -> symbol.equals(candidate.getSymbol()))
                .findFirst()
                .map(BinanceSpotRestClient::toSymbolInfo)
                .orElseThrow(() -> new IllegalStateException("Binance symbol information not found for " + symbol));
    }

    static BinanceSymbolInfo toSymbolInfo(ExchangeInfoResponseSymbolsInner symbol) {
        LotSizeFilter lotSize = symbol.getFilters().stream()
                .map(filter -> filter.getActualInstance())
                .filter(LotSizeFilter.class::isInstance)
                .map(LotSizeFilter.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Binance LOT_SIZE filter not found for " + symbol.getSymbol()));
        return new BinanceSymbolInfo(
                symbol.getSymbol(),
                symbol.getBaseAsset(),
                symbol.getQuoteAsset(),
                new BinanceLotSize(decimalValue(lotSize.getMinQty()), decimalValue(lotSize.getStepSize())));
    }

    @Override
    public List<BinanceKline> loadKlines(String symbol, String timeframe, int limit) {
        if (limit <= 0 || limit > 1000) {
            throw new IllegalArgumentException("Binance kline limit must be between 1 and 1000.");
        }
        var response = callApi(
                () -> spotRestApi.klines(symbol, Interval.fromValue(timeframe), null, null, null, limit).getData(),
                "Failed to load Binance klines for symbol " + symbol);
        return response.stream().map(item -> new BinanceKline(
                offsetDateTime(item.get(0)),
                offsetDateTime(item.get(6)),
                decimal(item.get(1)),
                decimal(item.get(2)),
                decimal(item.get(3)),
                decimal(item.get(4)),
                decimal(item.get(5)))).toList();
    }

    @Override
    public List<BinanceBalance> loadBalances(double recvWindowMillis) {
        var response = callApi(
                () -> spotRestApi.getAccount(null, recvWindowMillis).getData(),
                "Failed to load Binance account balances");
        return response.getBalances().stream()
                .map(balance -> new BinanceBalance(
                        balance.getAsset(), decimal(balance.getFree()), decimal(balance.getLocked())))
                .toList();
    }

    @Override
    public List<BinanceOrder> loadOrders(String symbol, double recvWindowMillis) {
        var response = callApi(
                () -> spotRestApi.allOrders(symbol, null, null, null, 1000, recvWindowMillis).getData(),
                "Failed to load Binance orders for symbol " + symbol);
        if (response.size() == 1000) {
            throw new IllegalStateException(
                    "Binance returned 1000 orders for %s; pagination is required before reconciliation can continue."
                            .formatted(symbol));
        }
        return response.stream().map(order -> new BinanceOrder(
                order.getOrderId(),
                order.getClientOrderId(),
                order.getSymbol(),
                toAction(order.getSide()),
                order.getStatus(),
                decimal(order.getOrigQty()),
                decimal(order.getExecutedQty()),
                decimal(order.getCummulativeQuoteQty()),
                offsetDateTime(order.getUpdateTime()))).toList();
    }

    @Override
    public Optional<BinanceOrder> findOrder(String symbol, String clientOrderId, double recvWindowMillis) {
        try {
            var order = spotRestApi.getOrder(symbol, null, clientOrderId, recvWindowMillis).getData();
            return Optional.of(new BinanceOrder(
                    order.getOrderId(),
                    order.getClientOrderId(),
                    order.getSymbol(),
                    toAction(order.getSide()),
                    order.getStatus(),
                    decimal(order.getOrigQty()),
                    decimal(order.getExecutedQty()),
                    decimal(order.getCummulativeQuoteQty()),
                    offsetDateTime(order.getUpdateTime())));
        } catch (ApiException exception) {
            if (isOrderNotFound(exception)) {
                return Optional.empty();
            }
            throw new IllegalStateException(
                    "Failed to find Binance order %s for symbol %s".formatted(clientOrderId, symbol), exception);
        }
    }

    static boolean isOrderNotFound(ApiException exception) {
        try {
            return OBJECT_MAPPER.readTree(exception.getResponseBody()).path("code").asInt()
                    == ORDER_NOT_FOUND_CODE;
        } catch (JsonProcessingException | IllegalArgumentException exceptionIgnored) {
            return false;
        }
    }

    @Override
    public List<BinanceFill> loadTrades(String symbol, double recvWindowMillis) {
        var response = callApi(
                () -> spotRestApi.myTrades(symbol, null, null, null, null, 1000, recvWindowMillis).getData(),
                "Failed to load Binance trades for symbol " + symbol);
        if (response.size() == 1000) {
            throw new IllegalStateException(
                    "Binance returned 1000 trades for %s; pagination is required before reconciliation can continue."
                            .formatted(symbol));
        }
        return response.stream().map(trade -> new BinanceFill(
                trade.getId(),
                trade.getOrderId(),
                trade.getSymbol(),
                Boolean.TRUE.equals(trade.getIsBuyer()) ? TradeAction.BUY : TradeAction.SELL,
                decimal(trade.getPrice()),
                decimal(trade.getQty()),
                decimal(trade.getQuoteQty()),
                decimal(trade.getCommission()),
                trade.getCommissionAsset(),
                offsetDateTime(trade.getTime()))).toList();
    }

    @Override
    public void validateOrder(OrderTestRequest request) {
        runApiCall(
                () -> spotRestApi.orderTest(request),
                "Binance order validation failed for symbol " + request.getSymbol());
    }

    @Override
    public NewOrderResponse placeOrder(NewOrderRequest request) {
        return callApi(
                () -> spotRestApi.newOrder(request).getData(),
                "Binance order placement failed for symbol " + request.getSymbol());
    }

    private void runApiCall(ApiRunnable action, String failureMessage) {
        try {
            action.run();
        } catch (ApiException exception) {
            throw new IllegalStateException(failureMessage, exception);
        }
    }

    private <T> T callApi(Supplier<T> action, String failureMessage) {
        try {
            return action.get();
        } catch (ApiException exception) {
            throw new IllegalStateException(failureMessage, exception);
        }
    }

    private BigDecimal decimal(String value) {
        return decimalValue(value);
    }

    private static BigDecimal decimalValue(String value) {
        return new BigDecimal(value);
    }

    private java.time.OffsetDateTime offsetDateTime(String epochMillis) {
        return offsetDateTime(Long.parseLong(epochMillis));
    }

    private java.time.OffsetDateTime offsetDateTime(Long epochMillis) {
        if (epochMillis == null) {
            throw new IllegalStateException("Binance response is missing a required timestamp.");
        }
        return Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC);
    }

    private TradeAction toAction(String side) {
        return "BUY".equals(side) ? TradeAction.BUY : TradeAction.SELL;
    }

    @FunctionalInterface
    private interface ApiRunnable {
        void run() throws ApiException;
    }

    @FunctionalInterface
    private interface Supplier<T> {
        T get() throws ApiException;
    }
}
