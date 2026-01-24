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

    public OperationResult executeSpotMarketBuy(Symbol symbol, BigDecimal percentOfDeposit) {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("💰 Выполняется рыночная покупка: {}, процент от депозита в {}: {}%",
                    symbol.asPair(), symbol.quote(), percentOfDeposit);
            OrderExecutionResult result = exchange.placeSpotMarketBuy(symbol, percentOfDeposit);
            return OperationResult.success(result);
        } catch (Exception e) {
            log.error("❌ Ошибка при выполнении покупки: {}", e.getMessage(), e);
            return OperationResult.error("ORDER_EXECUTION_FAILED", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public OperationResult executeSpotMarketSell(Symbol symbol, BigDecimal percentOfDeposit) {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("💰 Выполняется рыночная продажа: {}, процент от депозита в {}: {}%",
                    symbol.asPair(), symbol.base(), percentOfDeposit);
            OrderExecutionResult result = exchange.placeSpotMarketSell(symbol, percentOfDeposit);
            return OperationResult.success(result);
        } catch (Exception e) {
            log.error("❌ Ошибка при выполнении продажи: {}", e.getMessage(), e);
            return OperationResult.error("ORDER_EXECUTION_FAILED", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Получает текущую цену символа в квотируемой валюте
     * @param symbol Торговая пара
     * @return Текущая цена или null в случае ошибки
     */
    public BigDecimal getCurrentPrice(Symbol symbol) {
        try {
            log.debug("💹 Получение текущей цены для: {}", symbol.asPair());
            return exchange.getCurrentPrice(symbol);
        } catch (Exception e) {
            log.error("❌ Ошибка при получении цены: {}", e.getMessage(), e);
            return null;
        }
    }
}
