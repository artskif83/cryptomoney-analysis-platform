package artskif.trader.api;

import artskif.trader.api.dto.MarketOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * API интерфейс для взаимодействия с сервисом Trading Executor
 */
@Path("/api/trading")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface TradingExecutorApi {

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

