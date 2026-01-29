package artskif.trader.broker.client;

import artskif.trader.api.dto.MarketOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.api.dto.TradingResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

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
     * открыть лонг позицию
     * @throws TradingExecutionException если произошла ошибка при выполнении ордера
     */
    public OrderExecutionResult openLong(String instrument, BigDecimal persentOfDeposit) {
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
     * открыть шорт позицию
     * @throws TradingExecutionException если произошла ошибка при выполнении ордера
     */
    public OrderExecutionResult openShort(String instrument, BigDecimal persentOfDeposit) {
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

    public void closeShortPositions() {
        
    }

    public void closeLongPositions() {
    }
}

