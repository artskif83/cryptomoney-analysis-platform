package artskif.trader.broker.client;

import artskif.trader.api.dto.MarketOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

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
     * @return результат выполнения ордера
     */
    @POST
    @Path("/buy")
    OrderExecutionResult placeMarketBuy(MarketOrderRequest request);

    /**
     * Выполнить рыночную продажу
     * @param request запрос с параметрами ордера
     * @return результат выполнения ордера
     */
    @POST
    @Path("/sell")
    OrderExecutionResult placeMarketSell(MarketOrderRequest request);
}
