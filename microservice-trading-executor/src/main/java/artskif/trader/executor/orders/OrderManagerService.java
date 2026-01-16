package artskif.trader.executor.orders;


import artskif.trader.executor.orders.positions.ExchangeClient;
import artskif.trader.executor.orders.positions.OrderExecutionResult;
import artskif.trader.executor.orders.model.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public final class OrderManagerService {

    private static final Logger log = LoggerFactory.getLogger(OrderManagerService.class);

    private final ExchangeClient exchange;
    private final ConcurrentHashMap<String, ReentrantLock> symbolLocks = new ConcurrentHashMap<>();

    public OrderManagerService(ExchangeClient exchange) {
        this.exchange = exchange;
    }

    public OrderExecutionResult executeMarketBuy(Symbol symbol, BigDecimal baseQty) {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("Выполняется рыночная покупка: {}, количество: {}", symbol.asPair(), baseQty);
            return exchange.placeMarketBuy(symbol, baseQty);
        } finally {
            lock.unlock();
        }
    }

    public OrderExecutionResult executeMarketSell(Symbol symbol, BigDecimal baseQty) {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("Выполняется рыночная продажа: {}, количество: {}", symbol.asPair(), baseQty);
            return exchange.placeMarketSell(symbol, baseQty);
        } finally {
            lock.unlock();
        }
    }
}
