package artskif.trader.executor.rest;

import artskif.trader.api.TradingExecutorApi;
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
}

