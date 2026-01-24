package artskif.trader.api;

import artskif.trader.api.dto.MarketOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.api.dto.TradingResponse;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;

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
