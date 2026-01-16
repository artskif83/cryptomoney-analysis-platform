package artskif.trader.executor.orders.positions;

import artskif.trader.executor.orders.model.Symbol;

import java.math.BigDecimal;

public interface ExchangeClient {
    OrderExecutionResult placeMarketBuy(Symbol symbol, BigDecimal baseQty);
    OrderExecutionResult placeMarketSell(Symbol symbol, BigDecimal baseQty);
}
