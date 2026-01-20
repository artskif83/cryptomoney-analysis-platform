package artskif.trader.executor.rest;

import artskif.trader.executor.orders.OrderManagerService;
import artskif.trader.executor.common.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/trading")
public class OrdersController {

    private static final Logger log = LoggerFactory.getLogger(OrdersController.class);

    private final OrderManagerService orderManagerService;

    public OrdersController(OrderManagerService orderManagerService) {
        this.orderManagerService = orderManagerService;
    }

    @PostMapping("/buy")
    public OrderExecutionResult placeMarketBuy(@RequestBody MarketOrderRequest request) {
        log.info("📥 Получен запрос на покупку: {} - {}, количество: {}",
                request.base(), request.quote(), request.quantity());
        Symbol symbol = new Symbol(request.base(), request.quote());
        OrderExecutionResult result = orderManagerService.executeMarketBuy(symbol, request.quantity());
        log.info("✅ Покупка выполне��а: {}", result);
        return result;
    }

    @PostMapping("/sell")
    public OrderExecutionResult placeMarketSell(@RequestBody MarketOrderRequest request) {
        log.info("📥 Получен запрос на продажу: {} - {}, количество: {}",
                request.base(), request.quote(), request.quantity());
        Symbol symbol = new Symbol(request.base(), request.quote());
        OrderExecutionResult result = orderManagerService.executeMarketSell(symbol, request.quantity());
        log.info("✅ Продажа выполнена: {}", result);
        return result;
    }
}

