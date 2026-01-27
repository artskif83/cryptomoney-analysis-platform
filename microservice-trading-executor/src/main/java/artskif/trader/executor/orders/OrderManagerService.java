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

    /**
     * Размещает лимитный лонг-ордер на фьючерсном рынке
     * @param symbol Торговая пара
     * @param limitPrice Лимитная цена входа
     * @param positionSizeUsdt Размер позиции в USDT
     * @param stopLossPercent Процент отклонения для стоп-лосса
     * @param takeProfitPercent Процент отклонения для тейк-профита
     * @return Результат операции
     */
    public OperationResult executeFuturesLimitLong(Symbol symbol, BigDecimal limitPrice,
                                                   BigDecimal positionSizeUsdt,
                                                   BigDecimal stopLossPercent, BigDecimal takeProfitPercent) {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("📈 Выполняется фьючерсный лимитный лонг: {}, цена: {}, размер: {} USDT",
                    symbol.asPair(), limitPrice, positionSizeUsdt);
            OrderExecutionResult result = exchange.placeFuturesLimitLong(
                    symbol, limitPrice, positionSizeUsdt, stopLossPercent, takeProfitPercent);
            return OperationResult.success(result);
        } catch (Exception e) {
            log.error("❌ Ошибка при размещении фьючерсного лонг-ордера: {}", e.getMessage(), e);
            return OperationResult.error("ORDER_EXECUTION_FAILED", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Размещает лимитный шорт-ордер на фьючерсном рынке
     * @param symbol Торговая пара
     * @param limitPrice Лимитная цена входа
     * @param positionSizeUsdt Размер позиции в USDT
     * @param stopLossPercent Процент отклонения для стоп-лосса
     * @param takeProfitPercent Процент отклонения для тейк-профита
     * @return Результат операции
     */
    public OperationResult executeFuturesLimitShort(Symbol symbol, BigDecimal limitPrice,
                                                    BigDecimal positionSizeUsdt,
                                                    BigDecimal stopLossPercent, BigDecimal takeProfitPercent) {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("📉 Выполняется фьючерсный лимитный шорт: {}, цена: {}, размер: {} USDT",
                    symbol.asPair(), limitPrice, positionSizeUsdt);
            OrderExecutionResult result = exchange.placeFuturesLimitShort(
                    symbol, limitPrice, positionSizeUsdt, stopLossPercent, takeProfitPercent);
            return OperationResult.success(result);
        } catch (Exception e) {
            log.error("❌ Ошибка при размещении фьючерсного шорт-ордера: {}", e.getMessage(), e);
            return OperationResult.error("ORDER_EXECUTION_FAILED", e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
