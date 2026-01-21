package artskif.trader.broker.client;

import artskif.trader.api.dto.MarketOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
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
     * Выполнить рыночную покупку
     */
    public OrderExecutionResult executeBuy(String base, String quote, BigDecimal quantity) {
        log.info("🔄 Отправка запроса на покупку: {}/{} количество: {}", base, quote, quantity);

        MarketOrderRequest request = new MarketOrderRequest(base, quote, quantity);
        OrderExecutionResult result = executorClient.placeMarketBuy(request);

        log.info("✅ Покупка выполнена: orderId={}, avgPrice={}, executedQty={}",
                result.exchangeOrderId(), result.avgPrice(), result.executedBaseQty());

        return result;
    }

    /**
     * Выполнить рыночную продажу
     */
    public OrderExecutionResult executeSell(String base, String quote, BigDecimal quantity) {
        log.info("🔄 Отправка запроса на продажу: {}/{} количество: {}", base, quote, quantity);

        MarketOrderRequest request = new MarketOrderRequest(base, quote, quantity);
        OrderExecutionResult result = executorClient.placeMarketSell(request);

        log.info("✅ Продажа выполнена: orderId={}, avgPrice={}, executedQty={}",
                result.exchangeOrderId(), result.avgPrice(), result.executedBaseQty());

        return result;
    }
}

