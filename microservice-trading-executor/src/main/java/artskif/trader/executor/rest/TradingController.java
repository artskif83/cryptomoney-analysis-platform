package artskif.trader.executor.rest;

import artskif.trader.api.TradingExecutorApi;
import artskif.trader.api.dto.FuturesLimitOrderRequest;
import artskif.trader.api.dto.MarketOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.api.dto.TradingResponse;
import artskif.trader.executor.orders.AccountManagerService;
import artskif.trader.executor.orders.OperationResult;
import artskif.trader.executor.orders.OrderManagerService;
import artskif.trader.executor.common.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/trading")
public class TradingController implements TradingExecutorApi {

    private static final Logger log = LoggerFactory.getLogger(TradingController.class);

    private final OrderManagerService orderManagerService;
    private final AccountManagerService accountManagerService;

    public TradingController(OrderManagerService orderManagerService, AccountManagerService accountManagerService) {
        this.orderManagerService = orderManagerService;
        this.accountManagerService = accountManagerService;
    }

    @Override
    @PostMapping("/buy")
    public TradingResponse<OrderExecutionResult> placeSpotMarketBuy(@RequestBody MarketOrderRequest request) {
        log.info("📥 Получен запрос на покупку: инструмент {}, процент депозита: {}",
                request.instrument(), request.persentOfDeposit());

        try {
            Symbol symbol = Symbol.fromInstrument(request.instrument());
            OperationResult operationResult = orderManagerService.executeSpotMarketBuy(symbol, request.persentOfDeposit());

            return operationResult.map(
                    result -> {
                        log.info("✅ Покупка выполнена: {}", result);
                        return TradingResponse.success(result);
                    },
                    error -> {
                        log.error("❌ Ошибка при покупке: {} - {}", error.code(), error.message());
                        return TradingResponse.error(error.code(), error.message());
                    }
            );
        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при обработке запроса на покупку: {}", e.getMessage(), e);
            return TradingResponse.error("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @Override
    @PostMapping("/sell")
    public TradingResponse<OrderExecutionResult> placeSpotMarketSell(@RequestBody MarketOrderRequest request) {
        log.info("📥 Получен запрос на продажу: инструмент {}, процент депозита: {}",
                request.instrument(), request.persentOfDeposit());

        try {
            Symbol symbol = Symbol.fromInstrument(request.instrument());
            OperationResult operationResult = orderManagerService.executeSpotMarketSell(symbol, request.persentOfDeposit());

            return operationResult.map(
                    result -> {
                        log.info("✅ Продажа выполнена: {}", result);
                        return TradingResponse.success(result);
                    },
                    error -> {
                        log.error("❌ Ошибка при продаже: {} - {}", error.code(), error.message());
                        return TradingResponse.error(error.code(), error.message());
                    }
            );
        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при обработке запроса на продажу: {}", e.getMessage(), e);
            return TradingResponse.error("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @Override
    @GetMapping("/balance/usdt")
    public TradingResponse<BigDecimal> getUsdtBalance() {
        log.info("📥 Получен запрос на получение баланса USDT");

        try {
            BigDecimal balance = accountManagerService.getUsdtBalance();

            if (balance != null) {
                log.info("✅ Баланс USDT: {}", balance);
                return TradingResponse.success(balance);
            } else {
                log.error("❌ Не удалось получить баланс USDT");
                return TradingResponse.error("BALANCE_RETRIEVAL_FAILED", "Не удалось получить баланс USDT");
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при получении баланса USDT: {}", e.getMessage(), e);
            return TradingResponse.error("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @Override
    @GetMapping("/price/{instrument}")
    public TradingResponse<BigDecimal> getCurrentPrice(@PathVariable("instrument") String instrument) {
        log.info("📥 Получен запрос на получение текущей цены для инструмента: {}", instrument);

        try {
            Symbol symbol = Symbol.fromInstrument(instrument);
            BigDecimal price = orderManagerService.getCurrentPrice(symbol);

            if (price != null) {
                log.info("✅ Текущая цена {} = {}", symbol.asPair(), price);
                return TradingResponse.success(price);
            } else {
                log.error("❌ Не удалось получить текущую цену для {}", instrument);
                return TradingResponse.error("PRICE_RETRIEVAL_FAILED", "Не удалось получить текущую цену для " + instrument);
            }
        } catch (IllegalArgumentException e) {
            log.error("❌ Неверный формат инструмента: {}", instrument);
            return TradingResponse.error("INVALID_INSTRUMENT", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Ошибка при получении текущей цены: {}", e.getMessage(), e);
            return TradingResponse.error("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @Override
    @PostMapping("/futures/limit/long")
    public TradingResponse<OrderExecutionResult> placeFuturesLimitLong(@RequestBody FuturesLimitOrderRequest request) {
        log.info("📥 Получен запрос на фьючерсный лимитный лонг: инструмент {}, цена: {}, размер: {} USDT, SL: {}%, TP: {}%",
                request.instrument(), request.limitPrice(), request.positionSizeUsdt(),
                request.stopLossPercent(), request.takeProfitPercent());

        try {
            Symbol symbol = Symbol.fromInstrument(request.instrument());
            OperationResult operationResult = orderManagerService.executeFuturesLimitLong(
                    symbol,
                    request.limitPrice(),
                    request.positionSizeUsdt(),
                    request.stopLossPercent(),
                    request.takeProfitPercent()
            );

            return operationResult.map(
                    result -> {
                        log.info("✅ Фьючерсный лонг-ордер размещен: {}", result);
                        return TradingResponse.success(result);
                    },
                    error -> {
                        log.error("❌ Ошибка при размещении фьючерсного лонг-ордера: {} - {}", error.code(), error.message());
                        return TradingResponse.error(error.code(), error.message());
                    }
            );
        } catch (IllegalArgumentException e) {
            log.error("❌ Неверный формат инструмента: {}", request.instrument());
            return TradingResponse.error("INVALID_INSTRUMENT", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при размещении фьючерсного лонг-ордера: {}", e.getMessage(), e);
            return TradingResponse.error("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @Override
    @PostMapping("/futures/limit/short")
    public TradingResponse<OrderExecutionResult> placeFuturesLimitShort(@RequestBody FuturesLimitOrderRequest request) {
        log.info("📥 Получен запрос на фьючерсный лимитный шорт: инструмент {}, цена: {}, размер: {} USDT, SL: {}%, TP: {}%",
                request.instrument(), request.limitPrice(), request.positionSizeUsdt(),
                request.stopLossPercent(), request.takeProfitPercent());

        try {
            Symbol symbol = Symbol.fromInstrument(request.instrument());
            OperationResult operationResult = orderManagerService.executeFuturesLimitShort(
                    symbol,
                    request.limitPrice(),
                    request.positionSizeUsdt(),
                    request.stopLossPercent(),
                    request.takeProfitPercent()
            );

            return operationResult.map(
                    result -> {
                        log.info("✅ Фьючерсный шорт-ордер размещен: {}", result);
                        return TradingResponse.success(result);
                    },
                    error -> {
                        log.error("❌ Ошибка при размещении фьючерсного шорт-ордера: {} - {}", error.code(), error.message());
                        return TradingResponse.error(error.code(), error.message());
                    }
            );
        } catch (IllegalArgumentException e) {
            log.error("❌ Неверный формат инструмента: {}", request.instrument());
            return TradingResponse.error("INVALID_INSTRUMENT", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при размещении фьючерсного шорт-ордера: {}", e.getMessage(), e);
            return TradingResponse.error("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Получает список всех активных (ожидающих) SWAP ордеров
     * @param instId Опциональный параметр идентификатора инструмента (например, "BTC-USDT-SWAP")
     * @return Список активных SWAP ордеров
     */
    @GetMapping("/orders/pending")
    public TradingResponse<List<Map<String, Object>>> getPendingOrders(
            @RequestParam(value = "instId", required = false) String instId) {
        log.info("📥 Получен запрос на получение списка активных SWAP ордеров" +
                (instId != null ? " для " + instId : ""));

        try {
            List<Map<String, Object>> orders = orderManagerService.getPendingOrders(instId);

            log.info("✅ Получено {} активных SWAP ордеров", orders.size());
            return TradingResponse.success(orders);
        } catch (Exception e) {
            log.error("❌ Ошибка при получении списка активных SWAP ордеров: {}", e.getMessage(), e);
            return TradingResponse.error("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Отменяет ордера по заданным критериям.
     * @param ordId   Опциональный биржевой идентификатор ордера
     * @param clOrdId Опциональный клиентский идентификатор ордера
     * @return Результат операции отмены
     */
    @DeleteMapping("/orders/cancel")
    public TradingResponse<String> cancelOrders(
            @RequestParam(value = "ordId",   required = false) String ordId,
            @RequestParam(value = "clOrdId", required = false) String clOrdId) {
        boolean hasOrdId   = ordId   != null && !ordId.isEmpty();
        boolean hasClOrdId = clOrdId != null && !clOrdId.isEmpty();
        String desc = hasOrdId && hasClOrdId ? "ordId=" + ordId + " и clOrdId=" + clOrdId
                    : hasOrdId   ? "ordId=" + ordId
                    : hasClOrdId ? "clOrdId=" + clOrdId
                    : "все активные";
        log.info("📥 Получен запрос на отмену ордеров ({})", desc);

        try {
            boolean success = orderManagerService.cancelOrders(ordId, clOrdId);

            if (success) {
                String message = (!hasOrdId && !hasClOrdId)
                        ? "Все ордера успешно отменены"
                        : "Ордер (" + desc + ") успешно отменен";
                log.info("✅ Отмена ордеров выполнена: {}", message);
                return TradingResponse.success(message);
            } else {
                String message = (!hasOrdId && !hasClOrdId)
                        ? "Не удалось отменить все ордера"
                        : "Не удалось отменить ордер (" + desc + ")";
                log.error("❌ Ошибка при отмене ордеров: {}", message);
                return TradingResponse.error("ORDER_CANCELLATION_FAILED", message);
            }
        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при отмене ордеров: {}", e.getMessage(), e);
            return TradingResponse.error("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Получает список всех открытых позиций
     * @param instId Опциональный параметр идентификатора инструмента (например, "BTC-USDT-SWAP")
     * @return Список открытых позиций
     */
    @GetMapping("/positions")
    public TradingResponse<List<Map<String, Object>>> getPositions(
            @RequestParam(value = "instId", required = false) String instId) {
        log.info("📥 Получен запрос на получение списка открытых позиций" +
                (instId != null ? " для " + instId : ""));

        try {
            List<Map<String, Object>> positions = orderManagerService.getPositions(instId);

            log.info("✅ Получено {} открытых позиций", positions.size());
            return TradingResponse.success(positions);
        } catch (Exception e) {
            log.error("❌ Ошибка при получении списка позиций: {}", e.getMessage(), e);
            return TradingResponse.error("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Закрывает все текущие открытые позиции рыночным ордером
     * @param instId Опциональный параметр идентификатора инструмента для закрытия только конкретной позиции
     * @return Результат операции закрытия
     */
    @PostMapping("/positions/close")
    public TradingResponse<String> closeAllPositions(
            @RequestParam(value = "instId", required = false) String instId) {
        log.info("📥 Получен запрос на закрытие позиций" +
                (instId != null ? " для " + instId : " (все открытые)"));

        try {
            boolean success = orderManagerService.closeAllPositions(instId);

            if (success) {
                String message = instId != null
                        ? "Позиция для " + instId + " успешно закрыта"
                        : "Все позиции успешно закрыты";
                log.info("✅ Закрытие позиций выполнено: {}", message);
                return TradingResponse.success(message);
            } else {
                String message = instId != null
                        ? "Не удалось закрыть позицию для " + instId
                        : "Не удалось закрыть все позиции";
                log.error("❌ Ошибка при закрытии позиций: {}", message);
                return TradingResponse.error("POSITION_CLOSE_FAILED", message);
            }
        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при закрытии позиций: {}", e.getMessage(), e);
            return TradingResponse.error("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Получает историю закрытых SWAP позиций
     * @param instId Опциональный идентификатор инструмента (например, "BTC-USDT-SWAP")
     * @param before Опциональный Unix timestamp в мс — возвращать записи позже этого момента
     * @return Список записей истории позиций
     */
    @GetMapping("/positions/history")
    public TradingResponse<List<Map<String, Object>>> getPositionsHistory(
            @RequestParam(value = "instId", required = false) String instId,
            @RequestParam(value = "before", required = false) String before) {
        log.info("📥 Получен запрос на историю позиций: instId={}, before={}", instId, before);

        try {
            List<Map<String, Object>> history = accountManagerService.getPositionsHistory(instId, before);

            if (history != null) {
                log.info("✅ История позиций получена: {} записей", history.size());
                return TradingResponse.success(history);
            } else {
                log.error("❌ Не удалось получить историю позиций");
                return TradingResponse.error("POSITIONS_HISTORY_FAILED", "Не удалось получить историю позиций");
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при получении истории позиций: {}", e.getMessage(), e);
            return TradingResponse.error("INTERNAL_ERROR", "Внутренняя ошибка сервера: " + e.getMessage());
        }
    }
}


