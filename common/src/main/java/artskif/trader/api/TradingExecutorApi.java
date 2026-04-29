package artskif.trader.api;

import artskif.trader.api.dto.FuturesChaseOrderRequest;
import artskif.trader.api.dto.FuturesLimitOrderRequest;
import artskif.trader.api.dto.MarketOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.api.dto.TradingResponse;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    /**
     * Разместить Chase-ордер лонг на фьючерсном рынке.
     * Chase-ордер открывает позицию, conditional SL закрывает её.
     * @param request запрос с параметрами Chase-ордера
     * @return результат размещения ордера или ошибка
     */
    @POST
    @Path("/futures/chase/long")
    TradingResponse<OrderExecutionResult> placeFuturesChaseLong(FuturesChaseOrderRequest request);

    /**
     * Разместить Chase-ордер шорт на фьючерсном рынке.
     * Chase-ордер открывает позицию, conditional SL закрывает её.
     * @param request запрос с параметрами Chase-ордера
     * @return результат размещения ордера или ошибка
     */
    @POST
    @Path("/futures/chase/short")
    TradingResponse<OrderExecutionResult> placeFuturesChaseShort(FuturesChaseOrderRequest request);

    /**
     * Получает список всех активных (ожидающих) SWAP ордеров
     * @param instId Опциональный параметр идентификатора инструмента (например, "BTC-USDT-SWAP")
     * @return Список активных SWAP ордеров
     */
    @GET
    @Path("/orders/pending")
    TradingResponse<List<Map<String, Object>>> getPendingOrders(@QueryParam("instId") String instId);

    /**
     * Отменяет ордера по заданным критериям.
     * @param ordId   Опциональный биржевой идентификатор ордера
     * @param clOrdId Опциональный клиентский идентификатор ордера
     * @return Результат операции отмены
     */
    @DELETE
    @Path("/orders/cancel")
    TradingResponse<String> cancelOrders(@QueryParam("ordId") String ordId, @QueryParam("clOrdId") String clOrdId);

    /**
     * Получает список всех открытых позиций
     * @param instId Опциональный параметр идентификатора инструмента (например, "BTC-USDT-SWAP")
     * @return Список открытых позиций
     */
    @GET
    @Path("/positions")
    TradingResponse<List<Map<String, Object>>> getPositions(@QueryParam("instId") String instId);

    /**
     * Закрывает все текущие открытые позиции рыночным ордером
     * @param instId Опциональный параметр идентификатора инструмента для закрытия только конкретной позиции
     * @return Результат операции закрытия
     */
    @POST
    @Path("/positions/close")
    TradingResponse<String> closeAllPositions(@QueryParam("instId") String instId);

    /**
     * Получает историю закрытых SWAP позиций по инструменту.
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP"), опционально
     * @param before Unix timestamp в миллисекундах; возвращаются записи позже этого момента, опционально
     * @return Список записей истории позиций или ошибка
     */
    @GET
    @Path("/positions/history")
    TradingResponse<List<Map<String, Object>>> getPositionsHistory(
            @QueryParam("instId") String instId,
            @QueryParam("before") String before);
}
