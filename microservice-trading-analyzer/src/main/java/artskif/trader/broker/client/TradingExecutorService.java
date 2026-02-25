package artskif.trader.broker.client;

import artskif.trader.api.dto.FuturesLimitOrderRequest;
import artskif.trader.api.dto.MarketOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.api.dto.TradingResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Сервис для взаимодействия с Trading Executor
 * Предоставляет удобные методы для выполнения торговых операций
 */
@ApplicationScoped
public class TradingExecutorService {

    private static final Logger log = LoggerFactory.getLogger(TradingExecutorService.class);

    @Inject
    @RestClient
    TradingExecutorClient executorClient;

    /**
     * Выполнить рыночную покупку (открыть лонг позицию на споте)
     * @param instrument инструмент для торговли
     * @param persentOfDeposit процент от депозита для использования
     * @return результат выполнения ордера
     * @throws TradingExecutionException если произошла ошибка при выполнении ордера
     */
    public OrderExecutionResult placeSpotMarketBuy(String instrument, BigDecimal persentOfDeposit) {
        log.info("🔄 Отправка запроса на покупку: {} процент от депозита: {}", instrument, persentOfDeposit);

        MarketOrderRequest request = new MarketOrderRequest(instrument, persentOfDeposit);
        TradingResponse<OrderExecutionResult> response = executorClient.placeSpotMarketBuy(request);

        if (!response.success()) {
            log.error("❌ Ошибка при покупке: {} - {}", response.errorCode(), response.errorMessage());
            throw new TradingExecutionException(response.errorCode(), response.errorMessage());
        }

        OrderExecutionResult result = response.result();
        log.info("✅ Покупка выполнена: orderId={}, avgPrice={}, executedQty={}",
                result.exchangeOrderId(), result.avgPrice(), result.executedBaseQty());

        return result;
    }

    /**
     * Выполнить рыночную продажу (открыть шорт позицию на споте)
     * @param instrument инструмент для торговли
     * @param persentOfDeposit процент от депозита для использования
     * @return результат выполнения ордера
     * @throws TradingExecutionException если произошла ошибка при выполнении ордера
     */
    public OrderExecutionResult placeSpotMarketSell(String instrument, BigDecimal persentOfDeposit) {
        log.info("🔄 Отправка запроса на продажу: {} процент от депозита: {}", instrument, persentOfDeposit);

        MarketOrderRequest request = new MarketOrderRequest(instrument, persentOfDeposit);
        TradingResponse<OrderExecutionResult> response = executorClient.placeSpotMarketSell(request);

        if (!response.success()) {
            log.error("❌ Ошибка при продаже: {} - {}", response.errorCode(), response.errorMessage());
            throw new TradingExecutionException(response.errorCode(), response.errorMessage());
        }

        OrderExecutionResult result = response.result();
        log.info("✅ Продажа выполнена: orderId={}, avgPrice={}, executedQty={}",
                result.exchangeOrderId(), result.avgPrice(), result.executedBaseQty());

        return result;
    }

    /**
     * Получить баланс USDT
     * @return баланс USDT
     * @throws TradingExecutionException если произошла ошибка при получении баланса
     */
    public BigDecimal getUsdtBalance() {
        log.info("🔄 Запрос баланса USDT");

        TradingResponse<BigDecimal> response = executorClient.getUsdtBalance();

        if (!response.success()) {
            log.error("❌ Ошибка при получении баланса: {} - {}", response.errorCode(), response.errorMessage());
            throw new TradingExecutionException(response.errorCode(), response.errorMessage());
        }

        BigDecimal balance = response.result();
        log.info("✅ Баланс USDT: {}", balance);

        return balance;
    }

    /**
     * Получить текущую цену инструмента
     * @param instrument инструмент в формате BASE-QUOTE (например, BTC-USDT)
     * @return текущая цена
     * @throws TradingExecutionException если произошла ошибка при получении цены
     */
    public BigDecimal getCurrentPrice(String instrument) {
        log.info("🔄 Запрос текущей цены для: {}", instrument);

        TradingResponse<BigDecimal> response = executorClient.getCurrentPrice(instrument);

        if (!response.success()) {
            log.error("❌ Ошибка при получении цены: {} - {}", response.errorCode(), response.errorMessage());
            throw new TradingExecutionException(response.errorCode(), response.errorMessage());
        }

        BigDecimal price = response.result();
        log.info("✅ Текущая цена {}: {}", instrument, price);

        return price;
    }

    /**
     * Разместить лимитный лонг-ордер на фьючерсном рынке
     * @param request запрос с параметрами фьючерсного ордера
     * @return результат размещения ордера
     * @throws TradingExecutionException если произошла ошибка при размещении ордера
     */
    public OrderExecutionResult placeFuturesLimitLong(FuturesLimitOrderRequest request) {
        log.info("🔄 Размещение лимитного лонг-ордера: {}, цена: {}, размер: {} USDT",
                request.instrument(), request.limitPrice(), request.positionSizeUsdt());

        TradingResponse<OrderExecutionResult> response = executorClient.placeFuturesLimitLong(request);

        if (!response.success()) {
            log.error("❌ Ошибка при размещении лонг-ордера: {} - {}", response.errorCode(), response.errorMessage());
            throw new TradingExecutionException(response.errorCode(), response.errorMessage());
        }

        OrderExecutionResult result = response.result();
        log.info("✅ Лонг-ордер размещен: orderId={}, avgPrice={}, executedQty={}",
                result.exchangeOrderId(), result.avgPrice(), result.executedBaseQty());

        return result;
    }

    /**
     * Разместить лимитный шорт-ордер на фьючерсном рынке
     * @param request запрос с параметрами фьючерсного ордера
     * @return результат размещения ордера
     * @throws TradingExecutionException если произошла ошибка при размещении ордера
     */
    public OrderExecutionResult placeFuturesLimitShort(FuturesLimitOrderRequest request) {
        log.info("🔄 Размещение лимитного шорт-ордера: {}, цена: {}, размер: {} USDT",
                request.instrument(), request.limitPrice(), request.positionSizeUsdt());

        TradingResponse<OrderExecutionResult> response = executorClient.placeFuturesLimitShort(request);

        if (!response.success()) {
            log.error("❌ Ошибка при размещении шорт-ордера: {} - {}", response.errorCode(), response.errorMessage());
            throw new TradingExecutionException(response.errorCode(), response.errorMessage());
        }

        OrderExecutionResult result = response.result();
        log.info("✅ Шорт-ордер размещен: orderId={}, avgPrice={}, executedQty={}",
                result.exchangeOrderId(), result.avgPrice(), result.executedBaseQty());

        return result;
    }

    /**
     * Получить список активных (ожидающих) SWAP ордеров
     * @param instId опциональный идентификатор инструмента (например, "BTC-USDT-SWAP")
     * @return список активных SWAP ордеров
     * @throws TradingExecutionException если произошла ошибка при получении ордеров
     */
    public List<Map<String, Object>> getPendingOrders(String instId) {
        log.info("🔄 Запрос списка активных ордеров для: {}", instId != null ? instId : "всех инструментов");

        TradingResponse<List<Map<String, Object>>> response = executorClient.getPendingOrders(instId);

        if (!response.success()) {
            log.error("❌ Ошибка при получении ордеров: {} - {}", response.errorCode(), response.errorMessage());
            throw new TradingExecutionException(response.errorCode(), response.errorMessage());
        }

        List<Map<String, Object>> orders = response.result();
        log.info("✅ Получено активных ордеров: {}", orders.size());

        return orders;
    }

    /**
     * Отменить все ордера или конкретный ордер
     * @param clOrdId опциональный идентификатор ордера для отмены конкретного ордера
     * @return результат операции отмены
     * @throws TradingExecutionException если произошла ошибка при отмене ордеров
     */
    public String cancelOrders(String clOrdId) {
        log.info("🔄 Отмена ордеров: {}", clOrdId != null ? "orderId=" + clOrdId : "всех ордеров");

        TradingResponse<String> response = executorClient.cancelOrders(clOrdId);

        if (!response.success()) {
            log.error("❌ Ошибка при отмене ордеров: {} - {}", response.errorCode(), response.errorMessage());
            throw new TradingExecutionException(response.errorCode(), response.errorMessage());
        }

        String result = response.result();
        log.info("✅ Ордера отменены: {}", result);

        return result;
    }

    /**
     * Получить список открытых позиций
     * @param instId опциональный идентификатор инструмента (например, "BTC-USDT-SWAP")
     * @return список открытых позиций
     * @throws TradingExecutionException если произошла ошибка при получении позиций
     */
    public List<Map<String, Object>> getPositions(String instId) {
        log.info("🔄 Запрос списка открытых позиций для: {}", instId != null ? instId : "всех инструментов");

        TradingResponse<List<Map<String, Object>>> response = executorClient.getPositions(instId);

        if (!response.success()) {
            log.error("❌ Ошибка при получении позиций: {} - {}", response.errorCode(), response.errorMessage());
            throw new TradingExecutionException(response.errorCode(), response.errorMessage());
        }

        List<Map<String, Object>> positions = response.result();
        log.info("✅ Получено открытых позиций: {}", positions.size());

        return positions;
    }

    /**
     * Закрыть все открытые позиции рыночным ордером
     * @param instId опциональный идентификатор инструмента для закрытия только конкретной позиции
     * @return результат операции закрытия
     * @throws TradingExecutionException если произошла ошибка при закрытии позиций
     */
    public String closeAllPositions(String instId) {
        log.info("🔄 Закрытие позиций: {}", instId != null ? "instId=" + instId : "всех позиций");

        TradingResponse<String> response = executorClient.closeAllPositions(instId);

        if (!response.success()) {
            log.error("❌ Ошибка при закрытии позиций: {} - {}", response.errorCode(), response.errorMessage());
            throw new TradingExecutionException(response.errorCode(), response.errorMessage());
        }

        String result = response.result();
        log.info("✅ Позиции закрыты: {}", result);

        return result;
    }
}

