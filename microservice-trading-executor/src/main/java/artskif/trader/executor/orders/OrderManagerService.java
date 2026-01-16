package artskif.trader.executor.orders;


import artskif.trader.executor.orders.positions.ExchangeClient;
import artskif.trader.executor.orders.positions.OrderExecutionResult;
import artskif.trader.executor.orders.model.Symbol;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public final class OrderManagerService {

    private final ExchangeClient exchange;
    private final ConcurrentHashMap<String, ReentrantLock> symbolLocks = new ConcurrentHashMap<>();

    public OrderManagerService(ExchangeClient exchange) {
        this.exchange = exchange;
    }

    public OrderExecutionResult executeMarketBuy(Symbol symbol, BigDecimal baseQty) {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            return exchange.placeMarketBuy(symbol, baseQty);
        } finally {
            lock.unlock();
        }
    }

    public OrderExecutionResult executeMarketSell(Symbol symbol, BigDecimal baseQty) {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            return exchange.placeMarketSell(symbol, baseQty);
        } finally {
            lock.unlock();
        }
    }
}
