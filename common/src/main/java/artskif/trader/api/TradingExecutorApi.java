package artskif.trader.api;

import artskif.trader.api.dto.FuturesLimitOrderRequest;
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

    /**
     * Получить текущую цену символа в квотируемой валюте
     * @param instrument Инструмент в формате BASE-QUOTE (например, BTC-USDT)
     * @return текущая цена или ошибка
     */
    @GET
    @Path("/price/{instrument}")
    TradingResponse<BigDecimal> getCurrentPrice(@PathParam("instrument") String instrument);

    /**
     * Разместить лимитный лонг-ордер на фьючерсном рынке
     * @param request запрос с параметрами фьючерсного ордера
     * @return результат размещения ордера или ошибка
     */
    @POST
    @Path("/futures/limit/long")
    TradingResponse<OrderExecutionResult> placeFuturesLimitLong(FuturesLimitOrderRequest request);

    /**
     * Разместить лимитный шорт-ордер на фьючерсном рынке
     * @param request запрос с параметрами фьючерсного ордера
     * @return результат размещения ордера или ошибка
     */
    @POST
    @Path("/futures/limit/short")
    TradingResponse<OrderExecutionResult> placeFuturesLimitShort(FuturesLimitOrderRequest request);
}
