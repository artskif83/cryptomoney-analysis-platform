package artskif.trader.broker.client;

import artskif.trader.api.dto.MarketOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.api.dto.TradingResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.math.BigDecimal;

/**
 * REST клиент для взаимодействия с микросервисом Trading Executor
 * Использует общий API интерфейс из common модуля
 */
@Path("/api/trading")
@RegisterRestClient(configKey = "trading-executor")
public interface TradingExecutorClient {
    // Методы наследуются из TradingExecutorApi

    /**
     * Выполнить рыночную покупку
     * @param request запрос с параметрами ордера
     * @return результат выполнения ордера или ошибка
     */
    @POST
    @Path("/buy")
    TradingResponse<OrderExecutionResult> placeSpotMarketBuy(MarketOrderRequest request);

    /**
     * Выполнить рыночную продажу
     * @param request запрос с параметрами ордера
     * @return результат выполнения ордера или ошибка
     */
    @POST
    @Path("/sell")
    TradingResponse<OrderExecutionResult> placeSpotMarketSell(MarketOrderRequest request);

    /**
     * Получить баланс USDT
     * @return баланс USDT или ошибка
     */
    @GET
    @Path("/balance/usdt")
    TradingResponse<BigDecimal> getUsdtBalance();
}
