package ch.lueem.tradingbot.adapters.binance.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.binance.connector.client.common.ApiException;
import com.binance.connector.client.spot.rest.model.ExchangeInfoResponseSymbolsInner;
import com.binance.connector.client.spot.rest.model.LotSizeFilter;
import com.binance.connector.client.spot.rest.model.SymbolFilters;
import org.junit.jupiter.api.Test;

class BinanceSpotRestClientTest {

    @Test
    void mapsLotSizeFromExchangeInfo() {
        var symbol = new ExchangeInfoResponseSymbolsInner()
                .symbol("BTCUSDT")
                .baseAsset("BTC")
                .quoteAsset("USDT")
                .filters(java.util.List.of(new SymbolFilters(new LotSizeFilter()
                        .minQty("0.00001000")
                        .maxQty("9000.00000000")
                        .stepSize("0.00001000"))));

        var info = BinanceSpotRestClient.toSymbolInfo(symbol);

        assertEquals(new java.math.BigDecimal("0.00001000"), info.lotSize().minQty());
        assertEquals(new java.math.BigDecimal("0.00001000"), info.lotSize().stepSize());
    }

    @Test
    void recognizesBinanceOrderNotFoundCodeInHttp400ResponseBody() {
        var orderNotFound = new ApiException(
                400,
                Map.of(),
                "{\"code\":-2013,\"msg\":\"Order does not exist.\"}");
        var differentError = new ApiException(
                400,
                Map.of(),
                "{\"code\":-2010,\"msg\":\"Account has insufficient balance.\"}");

        assertTrue(BinanceSpotRestClient.isOrderNotFound(orderNotFound));
        assertFalse(BinanceSpotRestClient.isOrderNotFound(differentError));
    }
}
