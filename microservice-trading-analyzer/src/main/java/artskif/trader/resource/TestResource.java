package artskif.trader.resource;

import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.broker.client.TradingExecutorService;
import artskif.trader.candle.CandleEventType;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventBus;
import artskif.trader.events.regime.RegimeChangeEvent;
import artskif.trader.events.regime.RegimeChangeEventBus;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.strategy.event.common.Confidence;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventType;
import artskif.trader.strategy.regime.common.MarketRegime;
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
    RegimeChangeEventBus regimeChangeEventBus;

    @Inject
    TradeEventBus tradeEventBus;

    @Inject
    TradingExecutorService tradingExecutorService;

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
     * Симулировать событие REGIME_CHANGE
     *
     * @param instrument инструмент (например, BTC-USDT)
     * @param previousRegime предыдущий режим рынка
     * @param currentRegime текущий режим рынка
     * @return ответ с результатом симуляции
     */
    @POST
    @Path("/regime-change")
    public Response testRegimeChange(
            @QueryParam("instrument") @DefaultValue("BTC-USDT") String instrument,
            @QueryParam("previousRegime") @DefaultValue("FLAT") String previousRegime,
            @QueryParam("currentRegime") @DefaultValue("TREND_UP") String currentRegime
    ) {
        try {
            // Парсинг режимов
            MarketRegime prevRegime = MarketRegime.valueOf(previousRegime);
            MarketRegime currRegime = MarketRegime.valueOf(currentRegime);

            Instant timestamp = Instant.now();

            // Создание и публикация события
            RegimeChangeEvent event = new RegimeChangeEvent(
                    instrument,
                    prevRegime,
                    currRegime,
                    timestamp,
                    true // Тестовое событие
            );

            regimeChangeEventBus.publish(event);

            Log.infof("🔄 Событие REGIME_CHANGE симулировано: %s %s -> %s timestamp=%s (TEST)",
                    instrument, previousRegime, currentRegime, timestamp);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Событие REGIME_CHANGE успешно опубликовано",
                            "event", Map.of(
                                    "type", "REGIME_CHANGE",
                                    "instrument", instrument,
                                    "previousRegime", previousRegime,
                                    "currentRegime", currentRegime,
                                    "timestamp", timestamp.toString()
                            )
                    ))
                    .build();
        } catch (IllegalArgumentException e) {
            Log.errorf(e, "❌ Неверные параметры для симуляции REGIME_CHANGE");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "status", "error",
                            "message", "Неверные параметры. Доступные значения для режима: FLAT, TREND_UP, TREND_DOWN",
                            "previousRegime", previousRegime,
                            "currentRegime", currentRegime
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при симуляции события REGIME_CHANGE");
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
     * @param confidence уровень уверенности (LOW/MEDIUM/HIGH)
     * @param regime текущий режим рынка
     * @return ответ с результатом симуляции
     */
    @POST
    @Path("/trade-event")
    public Response testTradeEvent(
            @QueryParam("instrument") @DefaultValue("BTC-USDT") String instrument,
            @QueryParam("type") @DefaultValue("BREAKOUT") String type,
            @QueryParam("direction") @DefaultValue("LONG") String direction,
            @QueryParam("confidence") @DefaultValue("MEDIUM") String confidence,
            @QueryParam("regime") @DefaultValue("TREND_UP") String regime
    ) {
        try {
            // Парсинг параметров
            TradeEventType eventType = TradeEventType.valueOf(type);
            Direction eventDirection = Direction.valueOf(direction);
            Confidence eventConfidence = Confidence.valueOf(confidence);
            MarketRegime marketRegime = MarketRegime.valueOf(regime);

            Instant timestamp = Instant.now();

            // Создание и публикация события
            TradeEvent event = new TradeEvent(
                    eventType,
                    instrument,
                    eventDirection,
                    eventConfidence,
                    marketRegime,
                    timestamp,
                    true // Тестовое событие
            );

            tradeEventBus.publish(event);

            Log.infof("📈 Событие TRADE симулировано: %s %s %s %s режим=%s timestamp=%s (TEST)",
                    instrument, type, direction, confidence, regime, timestamp);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Событие TRADE успешно опубликовано",
                            "event", Map.of(
                                    "type", "TRADE",
                                    "instrument", instrument,
                                    "tradeEventType", type,
                                    "direction", direction,
                                    "confidence", confidence,
                                    "regime", regime,
                                    "timestamp", timestamp.toString()
                            )
                    ))
                    .build();
        } catch (IllegalArgumentException e) {
            Log.errorf(e, "❌ Неверные параметры для симуляции TRADE");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "status", "error",
                            "message", "Неверные параметры. Доступные значения: type=[PULLBACK,BREAKOUT,FALSE_BREAKOUT,EVENT_CANCELLED], direction=[LONG,SHORT], confidence=[LOW,MEDIUM,HIGH], regime=[FLAT,TREND_UP,TREND_DOWN]",
                            "type", type,
                            "direction", direction,
                            "confidence", confidence,
                            "regime", regime
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
     * @param base базовая валюта (например, BTC)
     * @param quote валюта котировки (например, USDT)
     * @param quantity количество базовой валюты для покупки
     * @return результат выполнения ордера
     */
    @POST
    @Path("/execute-buy")
    public Response testExecuteBuy(
            @QueryParam("base") @DefaultValue("BTC") String base,
            @QueryParam("quote") @DefaultValue("USDT") String quote,
            @QueryParam("quantity") @DefaultValue("0.001") BigDecimal quantity
    ) {
        try {
            Log.infof("🧪 Тестовый запрос на покупку: %s/%s количество: %s", base, quote, quantity);

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
            OrderExecutionResult result = tradingExecutorService.executeBuy(base, quote, quantity);

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
                                    "base", base,
                                    "quote", quote,
                                    "requestedQuantity", quantity.toString()
                            )
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при выполнении тестовой покупки %s/%s количество: %s",
                    base, quote, quantity);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "base", base,
                            "quote", quote,
                            "quantity", quantity != null ? quantity.toString() : "null"
                    ))
                    .build();
        }
    }

    /**
     * Тестовый endpoint для выполнения рыночной продажи
     *
     * @param base базовая валюта (например, BTC)
     * @param quote валюта котировки (например, USDT)
     * @param quantity количество базовой валюты для продажи
     * @return результат выполнения ордера
     */
    @POST
    @Path("/execute-sell")
    public Response testExecuteSell(
            @QueryParam("base") @DefaultValue("BTC") String base,
            @QueryParam("quote") @DefaultValue("USDT") String quote,
            @QueryParam("quantity") @DefaultValue("0.001") BigDecimal quantity
    ) {
        try {
            Log.infof("🧪 Тестовый запрос на продажу: %s/%s количество: %s", base, quote, quantity);

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
            OrderExecutionResult result = tradingExecutorService.executeSell(base, quote, quantity);

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
                                    "base", base,
                                    "quote", quote,
                                    "requestedQuantity", quantity.toString()
                            )
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при выполнении тестовой продажи %s/%s количество: %s",
                    base, quote, quantity);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "base", base,
                            "quote", quote,
                            "quantity", quantity != null ? quantity.toString() : "null"
                    ))
                    .build();
        }
    }
}

