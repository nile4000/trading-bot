package ch.lueem.tradingbot.adapters.binance.client;

import java.util.List;

import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceBalance;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceFill;
import ch.lueem.tradingbot.adapters.execution.binance.model.BinanceOrder;

/** Loads Binance account state used for reconciliation. */
public interface BinanceAccountClient {

    List<BinanceBalance> loadBalances(double recvWindowMillis);

    List<BinanceOrder> loadOrders(String symbol, double recvWindowMillis);

    List<BinanceFill> loadTrades(String symbol, double recvWindowMillis);
}
