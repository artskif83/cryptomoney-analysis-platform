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
     * Отменяет все текущие ордера или конкретный ордер по его ID
     * @param clOrdId Опциональный параметр идентификатора ордера для отмены конкретного ордера
     * @return Результат операции отмены
     */
    @DeleteMapping("/orders/cancel")
    public TradingResponse<String> cancelOrders(
            @RequestParam(value = "clOrdId", required = false) String clOrdId) {
        log.info("📥 Получен запрос на отмену ордеров" +
                (clOrdId != null ? " с clOrdId: " + clOrdId : " (все активные)"));

        try {
            boolean success = orderManagerService.cancelOrders(clOrdId);

            if (success) {
                String message = clOrdId != null
                        ? "Ордер с ID " + clOrdId + " успешно отменен"
                        : "Все ордера успешно отменены";
                log.info("✅ Отмена ордеров выполнена: {}", message);
                return TradingResponse.success(message);
            } else {
                String message = clOrdId != null
                        ? "Не удалось отменить ордер с ID " + clOrdId
                        : "Не удалось отменить все ордера";
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
}


