package artskif.trader.microserviceokxexecutor.orders.positions;

import artskif.trader.microserviceokxexecutor.orders.signal.Symbol;

import java.math.BigDecimal;

public interface ExchangeClient {
    OrderExecutionResult placeMarketBuy(Symbol symbol, BigDecimal baseQty);
    OrderExecutionResult placeMarketSell(Symbol symbol, BigDecimal baseQty);
}
