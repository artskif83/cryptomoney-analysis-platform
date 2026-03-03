package artskif.trader.executor.orders;


import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.executor.common.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

    public OperationResult executeSpotMarketBuy(Symbol symbol, BigDecimal percentOfDeposit) throws Exception {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("💰 Выполняется рыночная покупка: {}, процент от депозита в {}: {}%",
                    symbol.asPair(), symbol.quote(), percentOfDeposit);
            OrderExecutionResult result = exchange.placeSpotMarketBuy(symbol, percentOfDeposit);
            return OperationResult.success(result);
        } finally {
            lock.unlock();
        }
    }

    public OperationResult executeSpotMarketSell(Symbol symbol, BigDecimal percentOfDeposit) throws Exception {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("💰 Выполняется рыночная продажа: {}, процент от депозита в {}: {}%",
                    symbol.asPair(), symbol.base(), percentOfDeposit);
            OrderExecutionResult result = exchange.placeSpotMarketSell(symbol, percentOfDeposit);
            return OperationResult.success(result);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Получает текущую цену символа в квотируемой валюте
     * @param symbol Торговая пара
     * @return Текущая цена
     */
    public BigDecimal getCurrentPrice(Symbol symbol) throws Exception {
        log.debug("💹 Получение текущей цены для: {}", symbol.asPair());
        return exchange.getCurrentPrice(symbol);
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
                                                   BigDecimal stopLossPercent, BigDecimal takeProfitPercent) throws Exception {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("📈 Выполняется фьючерсный лимитный лонг: {}, цена: {}, размер: {} USDT",
                    symbol.asPair(), limitPrice, positionSizeUsdt);
            OrderExecutionResult result = exchange.placeFuturesLimitLong(
                    symbol, limitPrice, positionSizeUsdt, stopLossPercent, takeProfitPercent);
            return OperationResult.success(result);
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
                                                    BigDecimal stopLossPercent, BigDecimal takeProfitPercent) throws Exception {
        var lock = symbolLocks.computeIfAbsent(symbol.asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            log.debug("📉 Выполняется фьючерсный лимитный шорт: {}, цена: {}, размер: {} USDT",
                    symbol.asPair(), limitPrice, positionSizeUsdt);
            OrderExecutionResult result = exchange.placeFuturesLimitShort(
                    symbol, limitPrice, positionSizeUsdt, stopLossPercent, takeProfitPercent);
            return OperationResult.success(result);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Получает список всех активных (ожидающих) SWAP ордеров
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP") или null для всех SWAP ордеров
     * @return Список активных SWAP ордеров
     */
    public List<Map<String, Object>> getPendingOrders(String instId) throws Exception {
        log.debug("📋 Получение списка активных SWAP ордеров" + (instId != null ? " для " + instId : ""));
        return exchange.getPendingLimitSwapOrders(instId);
    }

    /**
     * Отменяет ордера по заданным критериям.
     * @param ordId   Опциональный биржевой идентификатор ордера (ordId)
     * @param clOrdId Опциональный клиентский идентификатор ордера (clOrdId)
     * @return true если отмена прошла успешно, false в противном случае
     */
    public boolean cancelOrders(String ordId, String clOrdId) throws Exception {
        String desc = ordId != null && clOrdId != null
                ? "ordId=" + ordId + " и clOrdId=" + clOrdId
                : ordId != null ? "ordId=" + ordId
                : clOrdId != null ? "clOrdId=" + clOrdId
                : "всех активных";
        log.debug("🔄 Отмена ордеров ({})", desc);
        return exchange.cancelOrders(ordId, clOrdId);
    }

    /**
     * Получает список всех открытых позиций
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP") или null для всех позиций
     * @return Список открытых позиций
     */
    public List<Map<String, Object>> getPositions(String instId) throws Exception {
        log.debug("📋 Получение списка открытых позиций" + (instId != null ? " для " + instId : ""));
        return exchange.getPositions(instId);
    }

    /**
     * Закрывает все текущие открытые позиции рыночным ордером
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP") или null для закрытия всех позиций
     * @return true если все позиции успешно закрыты, false в противном случае
     */
    public boolean closeAllPositions(String instId) throws Exception {
        log.debug("🔄 Закрытие позиций" + (instId != null ? " для " + instId : " (все открытые)"));
        return exchange.closeAllPositions(instId);
    }
}
