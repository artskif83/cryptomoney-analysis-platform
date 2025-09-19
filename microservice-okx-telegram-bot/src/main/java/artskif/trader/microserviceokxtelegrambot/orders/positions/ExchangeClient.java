package artskif.trader.microserviceokxtelegrambot.orders.positions;

import artskif.trader.microserviceokxtelegrambot.orders.signal.Symbol;

import java.math.BigDecimal;

public interface ExchangeClient {
    OrderExecutionResult placeMarketBuy(Symbol symbol, BigDecimal baseQty);
    OrderExecutionResult placeMarketSell(Symbol symbol, BigDecimal baseQty);
}
