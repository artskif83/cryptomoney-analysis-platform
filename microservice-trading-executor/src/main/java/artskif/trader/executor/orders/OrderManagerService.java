package artskif.trader.executor.orders;


import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.executor.common.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public final class OrderManagerService {

    private static final Logger log = LoggerFactory.getLogger(OrderManagerService.class);

    private final OrdersClient exchange;
    private final ConcurrentHashMap<String, ReentrantLock> symbolLocks = new ConcurrentHashMap<>();

    public OrderManagerService(OrdersClient exchange) {
        this.exchange = exchange;
    }

    public OrderExecutionResult executeMarketBuy(Symbol symbol, BigDecimal quoteSz) {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("💰 Выполняется рыночная покупка: {}, количество в квотируемой валюте(USDT): {}", symbol.asPair(), quoteSz);
            return exchange.placeSpotMarketBuy(symbol, quoteSz);
        } finally {
            lock.unlock();
        }
    }

    public OrderExecutionResult executeMarketSell(Symbol symbol, BigDecimal quoteSz) {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("💰 Выполняется рыночная продажа: {}, количество в квотируемой валюте(USDT): {}", symbol.asPair(), quoteSz);
            return exchange.placeSpotMarketSell(symbol, quoteSz);
        } finally {
            lock.unlock();
        }
    }
}
