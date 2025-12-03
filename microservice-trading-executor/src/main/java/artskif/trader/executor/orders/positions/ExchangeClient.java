package artskif.trader.executor.orders.positions;

import artskif.trader.executor.orders.strategy.list.Symbol;

import java.math.BigDecimal;

public interface ExchangeClient {
    OrderExecutionResult placeMarketBuy(Symbol symbol, BigDecimal baseQty);
    OrderExecutionResult placeMarketSell(Symbol symbol, BigDecimal baseQty);
}
