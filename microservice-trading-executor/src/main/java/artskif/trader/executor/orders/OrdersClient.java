package artskif.trader.executor.orders;

import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.executor.common.Symbol;

import java.math.BigDecimal;

public interface OrdersClient {
    OrderExecutionResult placeMarketBuy(Symbol symbol, BigDecimal baseQty);
    OrderExecutionResult placeMarketSell(Symbol symbol, BigDecimal baseQty);
}
