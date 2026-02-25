package artskif.trader.resource;

import artskif.trader.api.dto.FuturesLimitOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.broker.AccountStateMonitor;
import artskif.trader.broker.AccountStateSnapshot;
import artskif.trader.broker.client.TradingExecutorService;
import artskif.trader.candle.CandleEventType;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventBus;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventData;
import artskif.trader.strategy.event.common.TradeEventType;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * REST API для тестирования событий и операций
 */
@Path("/api/test")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestResource {

    @Inject
    CandleEventBus candleEventBus;

    @Inject
    TradeEventBus tradeEventBus;

    @Inject
    TradingExecutorService tradingExecutorService;

    @Inject
    AccountStateMonitor accountStateMonitor;

    /**
     * Симулировать событие CANDLE_TICK
     *
     * @param instrument инструмент (например, BTC-USDT)
     * @param timeframe таймфрейм (1m, 5m, 4h, 1w)
     * @param open цена открытия
     * @param high максимальная цена
     * @param low минимальная цена
     * @param close цена закрытия
     * @param volume объем торгов
     * @param confirmed подтверждена ли свеча
     * @return ответ с результатом симуляции
     */
    @POST
    @Path("/candle-tick")
    public Response simulateCandleTick(
            @QueryParam("instrument") @DefaultValue("BTC-USDT") String instrument,
            @QueryParam("timeframe") @DefaultValue("CANDLE_5M") String timeframe,
            @QueryParam("open") @DefaultValue("50000") BigDecimal open,
            @QueryParam("high") @DefaultValue("51000") BigDecimal high,
            @QueryParam("low") @DefaultValue("49000") BigDecimal low,
            @QueryParam("close") @DefaultValue("50500") BigDecimal close,
            @QueryParam("volume") @DefaultValue("100") BigDecimal volume,
            @QueryParam("confirmed") @DefaultValue("false") Boolean confirmed
    ) {
        try {

            // Парсинг таймфрейма
            CandleTimeframe candleTimeframe;
            try {
                candleTimeframe = CandleTimeframe.fromString(timeframe);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Неверный таймфрейм. Доступные значения: 1m, 5m, 4h, 1w",
                                "timeframe", timeframe
                        ))
                        .build();
            }

            // Создание CandlestickDto
            CandlestickDto candlestickDto = new CandlestickDto();
            Instant bucket = Instant.now();
            candlestickDto.setTimestamp(bucket);
            candlestickDto.setOpen(open);
            candlestickDto.setHigh(high);
            candlestickDto.setLow(low);
            candlestickDto.setClose(close);
            candlestickDto.setVolume(volume);
            candlestickDto.setConfirmed(confirmed);
            candlestickDto.setPeriod(candleTimeframe);
            candlestickDto.setInstrument(instrument);

            // Создание и публикация события
            CandleEvent event = new CandleEvent(
                    CandleEventType.CANDLE_TICK,
                    candleTimeframe,
                    instrument,
                    bucket,
                    candlestickDto,
                    confirmed,
                    true // Тестовое событие
            );

            candleEventBus.publish(event);

            Log.infof("📊 Событие CANDLE_TICK симулировано: %s %s O=%s H=%s L=%s C=%s V=%s confirmed=%s (TEST)",
                    instrument, timeframe, open, high, low, close, volume, confirmed);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Событие CANDLE_TICK успешно опубликовано",
                            "event", Map.of(
                                    "type", "CANDLE_TICK",
                                    "instrument", instrument,
                                    "timeframe", timeframe,
                                    "bucket", bucket.toString(),
                                    "candle", Map.of(
                                            "open", open,
                                            "high", high,
                                            "low", low,
                                            "close", close,
                                            "volume", volume,
                                            "confirmed", confirmed
                                    )
                            )
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при симуляции события CANDLE_TICK");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Симулировать событие TRADE
     *
     * @param instrument инструмент (например, BTC-USDT)
     * @param type тип торгового события
     * @param direction направление (LONG/SHORT)
     * @param timeframe таймфрейм события
     * @param stopLossPercentage процент стоп-лосса
     * @param takeProfitPercentage процент тейк-профита
     * @param eventPrice цена события
     * @return ответ с результатом симуляции
     */
    @POST
    @Path("/trade-event")
    public Response testTradeEvent(
            @QueryParam("instrument") @DefaultValue("BTC-USDT") String instrument,
            @QueryParam("type") @DefaultValue("BREAKOUT") String type,
            @QueryParam("direction") @DefaultValue("LONG") String direction,
            @QueryParam("timeframe") @DefaultValue("5m") String timeframe,
            @QueryParam("tag") @DefaultValue("test-strategy") String tag,
            @QueryParam("stopLossPercentage") @DefaultValue("2.0") BigDecimal stopLossPercentage,
            @QueryParam("takeProfitPercentage") @DefaultValue("5.0") BigDecimal takeProfitPercentage,
            @QueryParam("eventPrice") @DefaultValue("50000") BigDecimal eventPrice
    ) {
        try {
            // Парсинг параметров
            TradeEventType eventType = TradeEventType.valueOf(type);
            Direction eventDirection = Direction.valueOf(direction);
            CandleTimeframe candleTimeframe = CandleTimeframe.fromString(timeframe);

            Instant timestamp = Instant.now();

            // Создание TradeEventData
            TradeEventData tradeEventData = new TradeEventData(
                    eventType,
                    eventDirection,
                    stopLossPercentage,
                    takeProfitPercentage,
                    candleTimeframe,
                    eventPrice
            );

            // Создание и публикация события
            TradeEvent event = new TradeEvent(
                    tradeEventData,
                    instrument,
                    tag,
                    timestamp,
                    true // Тестовое событие
            );

            tradeEventBus.publish(event);

            Log.infof("📈 Событие TRADE симулировано: %s %s %s %s tag=%s SL=%s%% TP=%s%% price=%s timestamp=%s (TEST)",
                    instrument, type, direction, timeframe, tag, stopLossPercentage, takeProfitPercentage, eventPrice, timestamp);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Событие TRADE успешно опубликовано",
                            "event", Map.of(
                                    "type", "TRADE",
                                    "instrument", instrument,
                                    "tradeEventType", type,
                                    "direction", direction,
                                    "timeframe", timeframe,
                                    "stopLossPercentage", stopLossPercentage.toString(),
                                    "takeProfitPercentage", takeProfitPercentage.toString(),
                                    "eventPrice", eventPrice.toString(),
                                    "timestamp", timestamp.toString()
                            )
                    ))
                    .build();
        } catch (IllegalArgumentException e) {
            Log.errorf(e, "❌ Неверные параметры для симуляции TRADE");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "status", "error",
                            "message", "Неверные параметры. Доступные значения: type=[PULLBACK,BREAKOUT,FALSE_BREAKOUT,EVENT_CANCELLED], direction=[LONG,SHORT], timeframe=[1m,5m,15m,1h,4h,1d,1w]",
                            "type", type,
                            "direction", direction,
                            "timeframe", timeframe
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при симуляции события TRADE");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Тестовый endpoint для выполнения рыночной покупки
     *
     * @param instrument валютная пара (например, BTC-USDT)
     * @param quantity количество базовой валюты для покупки
     * @return результат выполнения ордера
     */
    @POST
    @Path("/execute-buy")
    public Response testExecuteBuy(
            @QueryParam("base") @DefaultValue("BTC-USDT") String instrument,
            @QueryParam("quantity") @DefaultValue("0.001") BigDecimal quantity
    ) {
        try {
            Log.infof("🧪 Тестовый запрос на покупку: %s количество: %s", instrument, quantity);

            // Валидация параметров
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Количество должно быть больше нуля",
                                "quantity", quantity != null ? quantity.toString() : "null"
                        ))
                        .build();
            }

            // Выполнение покупки
            OrderExecutionResult result = tradingExecutorService.placeSpotMarketBuy(instrument, quantity);

            Log.infof("✅ Покупка выполнена: orderId=%s, avgPrice=%s, executedQty=%s",
                    result.exchangeOrderId(), result.avgPrice(), result.executedBaseQty());

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Покупка успешно выполнена",
                            "order", Map.of(
                                    "exchangeOrderId", result.exchangeOrderId(),
                                    "avgPrice", result.avgPrice().toString(),
                                    "executedBaseQty", result.executedBaseQty().toString(),
                                    "instrument", instrument,
                                    "requestedQuantity", quantity.toString()
                            )
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при выполнении тестовой покупки %s количество: %s",
                    instrument, quantity);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "instrument", instrument,
                            "quantity", quantity != null ? quantity.toString() : "null"
                    ))
                    .build();
        }
    }

    /**
     * Тестовый endpoint для выполнения рыночной продажи
     *
     * @param instrument валютная пара (например, BTC-USDT)
     * @param quantity количество базовой валюты для продажи
     * @return результат выполнения ордера
     */
    @POST
    @Path("/execute-sell")
    public Response testExecuteSell(
            @QueryParam("instrument") @DefaultValue("BTC") String instrument,
            @QueryParam("quantity") @DefaultValue("0.001") BigDecimal quantity
    ) {
        try {
            Log.infof("🧪 Тестовый запрос на продажу: %s количество: %s", instrument, quantity);

            // Валидация параметров
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Количество должно быть больше нуля",
                                "quantity", quantity != null ? quantity.toString() : "null"
                        ))
                        .build();
            }

            // Выполнение продажи
            OrderExecutionResult result = tradingExecutorService.placeSpotMarketSell(instrument, quantity);

            Log.infof("✅ Продажа выполнена: orderId=%s, avgPrice=%s, executedQty=%s",
                    result.exchangeOrderId(), result.avgPrice(), result.executedBaseQty());

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Продажа успешно выполнена",
                            "order", Map.of(
                                    "exchangeOrderId", result.exchangeOrderId(),
                                    "avgPrice", result.avgPrice().toString(),
                                    "executedBaseQty", result.executedBaseQty().toString(),
                                    "instrument", instrument,
                                    "requestedQuantity", quantity.toString()
                            )
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при выполнении тестовой продажи %s количество: %s",
                    instrument, quantity);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "base", instrument,
                            "quantity", quantity != null ? quantity.toString() : "null"
                    ))
                    .build();
        }
    }

    /**
     * Тестовый endpoint для получения баланса USDT
     *
     * @return текущий баланс USDT
     */
    @GET
    @Path("/balance")
    public Response testGetBalance() {
        try {
            Log.info("🧪 Тестовый запрос баланса USDT");

            BigDecimal balance = tradingExecutorService.getUsdtBalance();

            Log.infof("✅ Баланс USDT получен: %s", balance);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Баланс успешно получен",
                            "balance", balance.toString()
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при получении баланса USDT");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Тестовый endpoint для получения текущей цены инструмента
     *
     * @param instrument инструмент (например, BTC-USDT)
     * @return текущая цена инструмента
     */
    @GET
    @Path("/price")
    public Response testGetCurrentPrice(
            @QueryParam("instrument") @DefaultValue("BTC-USDT") String instrument
    ) {
        try {
            Log.infof("🧪 Тестовый запрос текущей цены для: %s", instrument);

            BigDecimal price = tradingExecutorService.getCurrentPrice(instrument);

            Log.infof("✅ Текущая цена %s получена: %s", instrument, price);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Цена успешно получена",
                            "instrument", instrument,
                            "price", price.toString()
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при получении цены для %s", instrument);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "instrument", instrument
                    ))
                    .build();
        }
    }

    /**
     * Тестовый endpoint для размещения лимитного лонг-ордера на фьючерсах
     *
     * @param instrument инструмент (например, BTC-USDT-SWAP)
     * @param limitPrice лимитная цена
     * @param positionSizeUsdt размер позиции в USDT
     * @param stopLossPercent процент стоп-лосса
     * @param takeProfitPercent процент тейк-профита
     * @return результат размещения ордера
     */
    @POST
    @Path("/futures/long")
    public Response testPlaceFuturesLimitLong(
            @QueryParam("instrument") @DefaultValue("BTC-USDT") String instrument,
            @QueryParam("limitPrice") @DefaultValue("50000") BigDecimal limitPrice,
            @QueryParam("positionSizeUsdt") @DefaultValue("100") BigDecimal positionSizeUsdt,
            @QueryParam("stopLossPercent") @DefaultValue("2.0") BigDecimal stopLossPercent,
            @QueryParam("takeProfitPercent") @DefaultValue("5.0") BigDecimal takeProfitPercent
    ) {
        try {
            Log.infof("🧪 Тестовый запрос на размещение лимитного лонг-ордера: %s цена: %s размер: %s USDT",
                    instrument, limitPrice, positionSizeUsdt);

            // Валидация параметров
            if (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Лимитная цена должна быть больше нуля"
                        ))
                        .build();
            }

            if (positionSizeUsdt == null || positionSizeUsdt.compareTo(BigDecimal.ZERO) <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Размер позиции должен быть больше нуля"
                        ))
                        .build();
            }

            FuturesLimitOrderRequest request = new FuturesLimitOrderRequest(
                    instrument,
                    limitPrice,
                    positionSizeUsdt,
                    stopLossPercent,
                    takeProfitPercent
            );

            OrderExecutionResult result = tradingExecutorService.placeFuturesLimitLong(request);

            Log.infof("✅ Лимитный лонг-ордер размещен: orderId=%s, avgPrice=%s, executedQty=%s",
                    result.exchangeOrderId(), result.avgPrice(), result.executedBaseQty());

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Лимитный лонг-ордер успешно размещен",
                            "order", Map.of(
                                    "exchangeOrderId", result.exchangeOrderId(),
                                    "avgPrice", result.avgPrice().toString(),
                                    "executedBaseQty", result.executedBaseQty().toString(),
                                    "instrument", instrument,
                                    "limitPrice", limitPrice.toString(),
                                    "positionSizeUsdt", positionSizeUsdt.toString()
                            )
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при размещении лимитного лонг-ордера %s", instrument);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "instrument", instrument
                    ))
                    .build();
        }
    }

    /**
     * Тестовый endpoint для размещения лимитного шорт-ордера на фьючерсах
     *
     * @param instrument инструмент (например, BTC-USDT-SWAP)
     * @param limitPrice лимитная цена
     * @param positionSizeUsdt размер позиции в USDT
     * @param stopLossPercent процент стоп-лосса
     * @param takeProfitPercent процент тейк-профита
     * @return результат размещения ордера
     */
    @POST
    @Path("/futures/short")
    public Response testPlaceFuturesLimitShort(
            @QueryParam("instrument") @DefaultValue("BTC-USDT-SWAP") String instrument,
            @QueryParam("limitPrice") @DefaultValue("50000") BigDecimal limitPrice,
            @QueryParam("positionSizeUsdt") @DefaultValue("100") BigDecimal positionSizeUsdt,
            @QueryParam("stopLossPercent") @DefaultValue("2.0") BigDecimal stopLossPercent,
            @QueryParam("takeProfitPercent") @DefaultValue("5.0") BigDecimal takeProfitPercent
    ) {
        try {
            Log.infof("🧪 Тестовый запрос на размещение лимитного шорт-ордера: %s цена: %s размер: %s USDT",
                    instrument, limitPrice, positionSizeUsdt);

            // Валидация параметров
            if (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Лимитная цена должна быть больше нуля"
                        ))
                        .build();
            }

            if (positionSizeUsdt == null || positionSizeUsdt.compareTo(BigDecimal.ZERO) <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Размер позиции должен быть больше нуля"
                        ))
                        .build();
            }

            FuturesLimitOrderRequest request = new FuturesLimitOrderRequest(
                    instrument,
                    limitPrice,
                    positionSizeUsdt,
                    stopLossPercent,
                    takeProfitPercent
            );

            OrderExecutionResult result = tradingExecutorService.placeFuturesLimitShort(request);

            Log.infof("✅ Лимитный шорт-ордер размещен: orderId=%s, avgPrice=%s, executedQty=%s",
                    result.exchangeOrderId(), result.avgPrice(), result.executedBaseQty());

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Лимитный шорт-ордер успешно размещен",
                            "order", Map.of(
                                    "exchangeOrderId", result.exchangeOrderId(),
                                    "avgPrice", result.avgPrice().toString(),
                                    "executedBaseQty", result.executedBaseQty().toString(),
                                    "instrument", instrument,
                                    "limitPrice", limitPrice.toString(),
                                    "positionSizeUsdt", positionSizeUsdt.toString()
                            )
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при размещении лимитного шорт-ордера %s", instrument);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "instrument", instrument
                    ))
                    .build();
        }
    }

    /**
     * Тестовый endpoint для получения списка активных ордеров
     *
     * @param instId опциональный идентификатор инструмента
     * @return список активных ордеров
     */
    @GET
    @Path("/orders/pending")
    public Response testGetPendingOrders(
            @QueryParam("instId") String instId
    ) {
        try {
            Log.infof("🧪 Тестовый запрос списка активных ордеров для: %s",
                    instId != null ? instId : "всех инструментов");

            var orders = tradingExecutorService.getPendingOrders(instId);

            Log.infof("✅ Получено активных ордеров: %d", orders.size());

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Список активных ордеров успешно получен",
                            "count", orders.size(),
                            "orders", orders
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при получении списка активных ордеров");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Тестовый endpoint для отмены ордеров
     *
     * @param clOrdId опциональный идентификатор ордера для отмены конкретного ордера
     * @return результат операции отмены
     */
    @DELETE
    @Path("/orders/cancel")
    public Response testCancelOrders(
            @QueryParam("clOrdId") String clOrdId
    ) {
        try {
            Log.infof("🧪 Тестовый запрос на отмену ордеров: %s",
                    clOrdId != null ? "orderId=" + clOrdId : "всех ордеров");

            String result = tradingExecutorService.cancelOrders(clOrdId);

            Log.infof("✅ Ордера отменены: %s", result);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Ордера успешно отменены",
                            "result", result
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при отмене ордеров");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Тестовый endpoint для получения списка открытых позиций
     *
     * @param instId опциональный идентификатор инструмента
     * @return список открытых позиций
     */
    @GET
    @Path("/positions")
    public Response testGetPositions(
            @QueryParam("instId") String instId
    ) {
        try {
            Log.infof("🧪 Тестовый запрос списка открытых позиций для: %s",
                    instId != null ? instId : "всех инструментов");

            var positions = tradingExecutorService.getPositions(instId);

            Log.infof("✅ Получено открытых позиций: %d", positions.size());

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Список открытых позиций успешно получен",
                            "count", positions.size(),
                            "positions", positions
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при получении списка открытых позиций");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Тестовый endpoint для закрытия всех позиций
     *
     * @param instId опциональный идентификатор инструмента для закрытия только конкретной позиции
     * @return результат операции закрытия
     */
    @POST
    @Path("/positions/close")
    public Response testCloseAllPositions(
            @QueryParam("instId") String instId
    ) {
        try {
            Log.infof("🧪 Тестовый запрос на закрытие позиций: %s",
                    instId != null ? "instId=" + instId : "всех позиций");

            String result = tradingExecutorService.closeAllPositions(instId);

            Log.infof("✅ Позиции закрыты: %s", result);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Позиции успешно закрыты",
                            "result", result
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при закрытии позиций");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Получить текущий снимок состояния аккаунта
     *
     * @return снимок состояния аккаунта с балансом, ордерами и позициями
     */
    @GET
    @Path("/account/snapshot")
    public Response getAccountSnapshot() {
        try {
            Log.info("🧪 Запрос текущего снимка состояния аккаунта");

            AccountStateSnapshot snapshot = accountStateMonitor.getCurrentSnapshot();

            if (snapshot == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Снимок состояния аккаунта еще не собран. Подождите минуту или вызовите /api/test/account/snapshot/force"
                        ))
                        .build();
            }

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "timestamp", snapshot.getTimestamp().toString(),
                            "usdtBalance", snapshot.getUsdtBalance(),
                            "pendingOrders", snapshot.getPendingOrders(),
                            "positions", snapshot.getPositions()
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при получении снимка состояния аккаунта");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Принудительно обновить снимок состояния аккаунта
     *
     * @return результат операции обновления
     */
    @POST
    @Path("/account/snapshot/force")
    public Response forceAccountSnapshot() {
        try {
            Log.info("🧪 Запрос принудительного обновления снимка состояния аккаунта");

            accountStateMonitor.forceCollect();

            AccountStateSnapshot snapshot = accountStateMonitor.getCurrentSnapshot();

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Снимок состояния аккаунта успешно обновлен",
                            "timestamp", snapshot.getTimestamp().toString(),
                            "usdtBalance", snapshot.getUsdtBalance(),
                            "pendingOrdersCount", snapshot.getPendingOrders().size(),
                            "positionsCount", snapshot.getPositions().size()
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при принудительном обновлении снимка состояния аккаунта");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }
}




