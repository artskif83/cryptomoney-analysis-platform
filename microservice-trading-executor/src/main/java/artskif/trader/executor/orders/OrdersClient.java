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
    OrderExecutionResult placeSpotMarketBuy(Symbol symbol, BigDecimal percentOfDeposit) throws Exception;

    /**
     * Размещает рыночный ордер на продажу на спотовом рынке
     * @param symbol Торговая пара
     * @param percentOfDeposit Процент от депозита в базовой валюте (от 0 до 100)
     * @return Результат исполнения ордера
     */
    OrderExecutionResult placeSpotMarketSell(Symbol symbol, BigDecimal percentOfDeposit) throws Exception;

    /**
     * Получает текущую цену символа в квотируемой валюте
     * @param symbol Торговая пара
     * @return Текущая цена символа
     */
    BigDecimal getCurrentPrice(Symbol symbol) throws Exception;

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
                                               BigDecimal stopLossPercent, BigDecimal takeProfitPercent) throws Exception;

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
                                                BigDecimal stopLossPercent, BigDecimal takeProfitPercent) throws Exception;

    /**
     * Размещает Chase-ордер лонг на фьючерсном рынке.
     * Chase-ордер открывает позицию, conditional SL закрывает её.
     * @param symbol Торговая пара
     * @param positionSizeUsdt Размер позиции в USDT
     * @param stopLossPercent Процент отклонения для стоп-лосса (например, 2.0 для 2%)
     * @return Результат размещения ордера (algoId Chase-ордера)
     */
    OrderExecutionResult placeFuturesChaseLong(Symbol symbol, BigDecimal positionSizeUsdt,
                                               BigDecimal stopLossPercent) throws Exception;

    /**
     * Размещает Chase-ордер шорт на фьючерсном рынке.
     * Chase-ордер открывает позицию, conditional SL закрывает её.
     * @param symbol Торговая пара
     * @param positionSizeUsdt Размер позиции в USDT
     * @param stopLossPercent Процент отклонения для стоп-лосса (например, 2.0 для 2%)
     * @return Результат размещения ордера (algoId Chase-ордера)
     */
    OrderExecutionResult placeFuturesChaseShort(Symbol symbol, BigDecimal positionSizeUsdt,
                                                BigDecimal stopLossPercent) throws Exception;

    /**
     * Получает список всех активных (ожидающих) SWAP ордеров
     * @return Список активных SWAP ордеров
     */
    java.util.List<java.util.Map<String, Object>> getPendingOrders() throws Exception;

    /**
     * Получает список всех активных (ожидающих) SWAP ордеров для указанного инструмента
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP")
     * @return Список активных SWAP ордеров
     */
    java.util.List<java.util.Map<String, Object>> getPendingLimitSwapOrders(String instId) throws Exception;

    /**
     * Отменяет все текущие ордера или конкретный ордер по его ID.
     * <ul>
     *   <li>Оба null — отменяются все активные ордера</li>
     *   <li>Только ordId не null — отменяется ордер по ordId</li>
     *   <li>Только clOrdId не null — отменяется ордер по clOrdId</li>
     *   <li>Оба не null — отменяется ордер, у которого совпадают оба значения</li>
     * </ul>
     * @param ordId   Опциональный биржевой идентификатор ордера (ordId)
     * @param clOrdId Опциональный клиентский идентификатор ордера (clOrdId)
     * @return true если отмена прошла успешно, false в противном случае
     */
    boolean cancelOrders(String ordId, String clOrdId) throws Exception;

    /**
     * Получает список всех открытых позиций
     * @param instId Опциональный идентификатор инструмента (например, "BTC-USDT-SWAP") для фильтрации позиций
     * @return Список открытых позиций
     */
    java.util.List<java.util.Map<String, Object>> getPositions(String instId) throws Exception;

    /**
     * Закрывает все текущие открытые позиции рыночным ордером
     * @param instId Опциональный идентификатор инструмента для закрытия только конкретной позиции (может быть null для закрытия всех позиций)
     * @return true если все позиции успешно закрыты, false в противном случае
     */
    boolean closeAllPositions(String instId) throws Exception;

    /**
     * Получает историю закрытых позиций по инструменту.
     * По умолчанию instType = SWAP, type = все закрытые позиции.
     *
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP")
     * @param before Фильтр по времени: возвращать записи с временем обновления строго позже
     *               этого значения (Unix timestamp в миллисекундах в виде строки)
     * @return Список записей истории позиций или null в случае ошибки
     */
    java.util.List<java.util.Map<String, Object>> getPositionsHistory(String instId, String before) throws Exception;
}
