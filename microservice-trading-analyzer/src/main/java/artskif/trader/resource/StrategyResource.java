package artskif.trader.resource;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventBus;
import artskif.trader.candle.CandleEventType;
import artskif.trader.events.regime.RegimeChangeEvent;
import artskif.trader.events.regime.RegimeChangeEventBus;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.strategy.event.common.Confidence;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventType;
import artskif.trader.strategy.regime.common.MarketRegime;
import artskif.trader.strategy.StrategyService;
import artskif.trader.strategy.contract.ContractDataService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * REST API для управления контрактами
 */
@Path("/api/strategy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StrategyResource {


    @Inject
    StrategyService strategyService;

    @Inject
    ContractDataService contractDataService;

    @Inject
    CandleEventBus candleEventBus;

    @Inject
    RegimeChangeEventBus regimeChangeEventBus;

    @Inject
    TradeEventBus tradeEventBus;

    /**
     * Запустить стратегию по имени
     * @param strategyName имя стратегии для запуска
     */
    @POST
    @Path("/start/{strategyName}")
    public Response startStrategy(@PathParam("strategyName") String strategyName) {
        try {
            Log.infof("🚀 Запрос на запуск стратегии: %s", strategyName);

            boolean success = strategyService.startStrategy(strategyName);

            if (success) {
                return Response.ok()
                        .entity(Map.of(
                                "status", "success",
                                "message", "Стратегия успешно запущена",
                                "strategyName", strategyName,
                                "running", true
                        ))
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Не удалось запустить стратегию (не найдена или уже запущена)",
                                "strategyName", strategyName
                        ))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при запуске стратегии: %s", strategyName);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "strategyName", strategyName
                    ))
                    .build();
        }
    }

    /**
     * Остановить стратегию по имени
     * @param strategyName имя стратегии для остановки
     */
    @POST
    @Path("/stop/{strategyName}")
    public Response stopStrategy(@PathParam("strategyName") String strategyName) {
        try {
            Log.infof("🛑 Запрос на остановку стратегии: %s", strategyName);

            boolean success = strategyService.stopStrategy(strategyName);

            if (success) {
                return Response.ok()
                        .entity(Map.of(
                                "status", "success",
                                "message", "Стратегия успешно остановлена",
                                "strategyName", strategyName,
                                "running", false
                        ))
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Не удалось остановить стратегию (не найдена или не запущена)",
                                "strategyName", strategyName
                        ))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при остановке стратегии: %s", strategyName);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "strategyName", strategyName
                    ))
                    .build();
        }
    }

    /**
     * Получить список всех зарегистрированных стратегий и их статусы
     */
    @GET
    @Path("/list")
    public Response getAllStrategies() {
        try {
            Map<String, Boolean> strategies = strategyService.getAllStrategies();

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "strategies", strategies
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при получении списка стратегий");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Получить статус конкретной стратегии
     * @param strategyName имя стратегии
     */
    @GET
    @Path("/status/{strategyName}")
    public Response getStrategyStatus(@PathParam("strategyName") String strategyName) {
        try {
            boolean isRunning = strategyService.isStrategyRunning(strategyName);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "strategyName", strategyName,
                            "running", isRunning
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при получении статуса стратегии: %s", strategyName);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "strategyName", strategyName
                    ))
                    .build();
        }
    }

    /**
     * Сгенерировать исторические фичи для всех контрактов
     */
    @POST
    @Path("/generate-historical")
    public Response generateHistoricalFeatures() {
        try {
            Log.infof("🚀 Запуск генерации исторических фич");

            // Генерируем исторические данные
            strategyService.generateHistoricalFeaturesForAll();

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Исторические фичи сгенерированы"
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Ошибка при генерации исторических фич");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Сгенерировать исторические фичи для одного контракта по его ID
     * @param contractId ID контракта
     */
    @POST
    @Path("/{contractId}/generate-historical")
    public Response generateHistoricalFeaturesForContract(
            @PathParam("contractId") Long contractId) {
        try {
            Log.infof("🚀 Запуск генерации исторических фич для контракта ID=%d",
                      contractId);

            // Получаем имя контракта по ID
            String contractName = strategyService.getContractNameById(contractId);
            if (contractName == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Контракт с указанным ID не найден",
                                "contractId", contractId
                        ))
                        .build();
            }

            // Генерируем исторические данные для контракта
            boolean success = strategyService.generateHistoricalFeaturesForContract(contractName);

            if (!success) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Контракт не найден в реестре",
                                "contractId", contractId,
                                "contractName", contractName
                        ))
                        .build();
            }

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Исторические фичи сгенерированы для контракта",
                            "contractId", contractId,
                            "contractName", contractName
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Ошибка при генерации исторических фич для контракта ID=%d", contractId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "contractId", contractId
                    ))
                    .build();
        }
    }

    /**
     * Вспомогательный метод для получения доступных значений таймфреймов
     */
    private String[] getCandleTimeframeValues() {
        CandleTimeframe[] values = CandleTimeframe.values();
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].name();
        }
        return result;
    }

    /**
     * Сгенерировать live фичи для всех контрактов
     */
    @POST
    @Path("/current-predict")
    public Response generatePredict() {
        try {
            strategyService.generatePredict();

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Live фичи сгенерированы"
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Ошибка при генерации live фич");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Удалить контракт со всеми его метаданными и зависимыми фичами по ID
     * @param contractId ID контракта для удаления
     * @return ответ с результатом удаления
     */
    @DELETE
    @Path("/{contractId}")
    public Response deleteContractById(@PathParam("contractId") Long contractId) {
        try {
            Log.infof("🗑️ Получен запрос на удаление контракта с ID: %d", contractId);

            boolean deleted = contractDataService.deleteContractById(contractId);

            if (deleted) {
                return Response.ok()
                        .entity(Map.of(
                                "status", "success",
                                "message", "Контракт успешно удален",
                                "contractId", contractId
                        ))
                        .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Контракт с указанным ID не найден",
                                "contractId", contractId
                        ))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при удалении контракта с ID: %d", contractId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "contractId", contractId
                    ))
                    .build();
        }
    }

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
    @Path("/simulate/candle-tick")
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
    @Path("/simulate/regime-change")
    public Response simulateRegimeChange(
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
    @Path("/simulate/trade-event")
    public Response simulateTradeEvent(
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
}

