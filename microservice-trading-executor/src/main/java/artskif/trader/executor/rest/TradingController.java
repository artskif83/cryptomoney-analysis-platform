package artskif.trader.executor.rest;

import artskif.trader.api.TradingExecutorApi;
import artskif.trader.api.dto.MarketOrderRequest;
import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.executor.orders.AccountManagerService;
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
    public OrderExecutionResult placeMarketBuy(@RequestBody MarketOrderRequest request) {
        log.info("📥 Получен запрос на покупку: инструмент {}, процент депозита: {}",
                request.instrument(), request.persentOfDeposit());
        Symbol symbol = Symbol.fromInstrument(request.instrument());
        OrderExecutionResult result = orderManagerService.executeMarketBuy(symbol, request.persentOfDeposit());
//        OrderExecutionResult result = new OrderExecutionResult("test-order-id-buy", request.persentOfDeposit(), request.persentOfDeposit());
        log.info("✅ Покупка выполнена: {}", result);
        return result;
    }

    @Override
    @PostMapping("/sell")
    public OrderExecutionResult placeMarketSell(@RequestBody MarketOrderRequest request) {
        log.info("📥 Получен запрос на продажу: инструмент {}, процент депозита: {}",
                request.instrument(), request.persentOfDeposit());
        Symbol symbol = Symbol.fromInstrument(request.instrument());
        OrderExecutionResult result = orderManagerService.executeMarketSell(symbol, request.persentOfDeposit());
//        OrderExecutionResult result = new OrderExecutionResult("test-order-id-sell", request.persentOfDeposit(), request.persentOfDeposit());
        log.info("✅ Продажа выполнена: {}", result);
        return result;
    }

    @GetMapping("/balance/usdt")
    public BigDecimal getUsdtBalance() {
        log.info("📥 Получен запрос на получение баланса USDT");
        BigDecimal balance = accountManagerService.getUsdtBalance();
        log.info("✅ Баланс USDT: {}", balance);
        return balance;
    }
}

