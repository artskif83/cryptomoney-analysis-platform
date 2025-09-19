package orders;

import artskif.trader.microserviceokxtelegrambot.orders.OrderManagerService;
import artskif.trader.microserviceokxtelegrambot.orders.positions.ExchangeClient;
import artskif.trader.microserviceokxtelegrambot.orders.positions.InMemoryUnitPositionStore;
import artskif.trader.microserviceokxtelegrambot.orders.positions.OrderExecutionResult;
import artskif.trader.microserviceokxtelegrambot.orders.positions.UnitPositionStore;
import artskif.trader.microserviceokxtelegrambot.orders.signal.Level;
import artskif.trader.microserviceokxtelegrambot.orders.signal.Side;
import artskif.trader.microserviceokxtelegrambot.orders.signal.Signal2;
import artskif.trader.microserviceokxtelegrambot.orders.signal.Symbol;
import artskif.trader.microserviceokxtelegrambot.orders.strategy.StrategyRegistry;
import artskif.trader.microserviceokxtelegrambot.orders.strategy.list.RsiStrategy;
import my.signals.v1.StrategyKind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderManagerServiceV2Test {

    static final Symbol BTCUSDT = new Symbol("BTC","USDT");

    @Test
    void buyThenSell_closesCheapestUnit_whenAbove1Percent() {
        UnitPositionStore store = new InMemoryUnitPositionStore();
        var rsi = new RsiStrategy(store, new BigDecimal("300"), new BigDecimal("0.01")); // 1 юнит = 300 USDT
        var registry = new StrategyRegistry(List.of(rsi));
        var exchange = new FakeExchange();
        var svc = new OrderManagerService(registry, exchange);

        // BUY STRONG → 4 инструкции, каждая baseQty = 300 / 20000 = 0.01500000
        svc.onSignal(new Signal2("s1", BTCUSDT, StrategyKind.ADX_RSI, Level.STRONG, Side.BUY,
                new BigDecimal("20000"), Instant.now()));
        assertEquals(4, store.usedUnits());

        // подложим один "дешёвый" юнит руками (эмулируем прошлую покупку по 19000)
        store.add(new UnitPositionStore.UnitPosition(BTCUSDT, new BigDecimal("19000"), new BigDecimal("0.01578947"), Instant.now()));
        assertEquals(5, store.usedUnits());

        // SELL при цене 19200 (< 19000*1.01=19190? нет, 19200 > 19190, значит продаём cheapest=19000)
        svc.onSignal(new Signal2("s2", BTCUSDT, StrategyKind.ADX_RSI, Level.MIDDLE, Side.SELL,
                new BigDecimal("19200"), Instant.now()));
        assertEquals(4, store.usedUnits()); // один юнит закрыт
        assertEquals(1, exchange.sellCount);
        assertEquals(new BigDecimal("0.01578947"), exchange.lastSellQty);
    }

    // простая мок-биржа
    static final class FakeExchange implements ExchangeClient {
        int buyCount = 0;
        int sellCount = 0;
        BigDecimal lastSellQty;

        @Override
        public OrderExecutionResult placeMarketBuy(Symbol symbol, BigDecimal baseQty) {
            buyCount++;
            return new OrderExecutionResult(UUID.randomUUID().toString(), new BigDecimal("20000"), baseQty);
        }
        @Override
        public OrderExecutionResult placeMarketSell(Symbol symbol, BigDecimal baseQty) {
            sellCount++;
            lastSellQty = baseQty;
            return new OrderExecutionResult(UUID.randomUUID().toString(), new BigDecimal("19200"), baseQty);
        }
    }
}
