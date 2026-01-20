package artskif.trader.executor.orders;

import artskif.trader.executor.common.Symbol;
import artskif.trader.executor.rest.OrderExecutionResult;

import java.math.BigDecimal;

public interface OrdersClient {
    OrderExecutionResult placeMarketBuy(Symbol symbol, BigDecimal baseQty);
    OrderExecutionResult placeMarketSell(Symbol symbol, BigDecimal baseQty);
}
