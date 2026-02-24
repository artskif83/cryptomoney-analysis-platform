package artskif.trader.executor.orders;

import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.executor.common.Symbol;

import java.math.BigDecimal;

public interface OrdersClient {
    /**
     * Размещает рыночный ордер на покупку на спотовом рынке
     * @param symbol Торговая пара
     * @param percentOfDeposit Процент от депозита в квотируемой валюте (от 0 до 100)
     * @return Результат исполнения ордера
     */
    OrderExecutionResult placeSpotMarketBuy(Symbol symbol, BigDecimal percentOfDeposit);

    /**
     * Размещает рыночный ордер на продажу на спотовом рынке
     * @param symbol Торговая пара
     * @param percentOfDeposit Процент от депозита в базовой валюте (от 0 до 100)
     * @return Результат исполнения ордера
     */
    OrderExecutionResult placeSpotMarketSell(Symbol symbol, BigDecimal percentOfDeposit);

    /**
     * Получает текущую цену символа в квотируемой валюте
     * @param symbol Торговая пара
     * @return Текущая цена символа или null в случае ошибки
     */
    BigDecimal getCurrentPrice(Symbol symbol);

    /**
     * Размещает лимитный лонг-ордер на фьючерсном рынке
     * @param symbol Торговая пара
     * @param limitPrice Лимитная цена входа
     * @param positionSizeUsdt Размер позиции в USDT
     * @param stopLossPercent Процент отклонения для стоп-лосса (например, 2.0 для 2%)
     * @param takeProfitPercent Процент отклонения для тейк-профита (например, 5.0 для 5%)
     * @return Результат размещения ордера
     */
    OrderExecutionResult placeFuturesLimitLong(Symbol symbol, BigDecimal limitPrice, BigDecimal positionSizeUsdt,
                                               BigDecimal stopLossPercent, BigDecimal takeProfitPercent);

    /**
     * Размещает лимитный шорт-ордер на фьючерсном рынке
     * @param symbol Торговая пара
     * @param limitPrice Лимитная цена входа
     * @param positionSizeUsdt Размер позиции в USDT
     * @param stopLossPercent Процент отклонения для стоп-лосса (например, 2.0 для 2%)
     * @param takeProfitPercent Процент отклонения для тейк-профита (например, 5.0 для 5%)
     * @return Результат размещения ордера
     */
    OrderExecutionResult placeFuturesLimitShort(Symbol symbol, BigDecimal limitPrice, BigDecimal positionSizeUsdt,
                                                BigDecimal stopLossPercent, BigDecimal takeProfitPercent);

    /**
     * Получает список всех активных (ожидающих) SWAP ордеров
     * @return Список активных SWAP ордеров или пустой список в случае ошибки
     */
    java.util.List<java.util.Map<String, Object>> getPendingOrders();

    /**
     * Получает список всех активных (ожидающих) SWAP ордеров для указанного инструмента
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP")
     * @return Список активных SWAP ордеров или пустой список в случае ошибки
     */
    java.util.List<java.util.Map<String, Object>> getPendingLimitSwapOrders(String instId);

    /**
     * Отменяет все текущие ордера или конкретный ордер по его ID
     * @param clOrdId Опциональный идентификатор ордера для отмены конкретного ордера (может быть null для отмены всех ордеров)
     * @return true если отмена прошла успешно, false в противном случае
     */
    boolean cancelOrders(String clOrdId);

    /**
     * Получает список всех открытых позиций
     * @param instId Опциональный идентификатор инструмента (например, "BTC-USDT-SWAP") для фильтрации позиций
     * @return Список открытых позиций или пустой список в случае ошибки
     */
    java.util.List<java.util.Map<String, Object>> getPositions(String instId);
}
