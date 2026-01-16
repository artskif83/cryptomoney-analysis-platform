package artskif.trader.executor.rest;

import artskif.trader.executor.orders.OrdersClient;
import artskif.trader.executor.orders.OrderExecutionResult;
import artskif.trader.executor.common.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/trading")
public class OrdersController {

    private static final Logger log = LoggerFactory.getLogger(OrdersController.class);

    private final OrdersClient ordersClient;

    public OrdersController(OrdersClient ordersClient) {
        this.ordersClient = ordersClient;
    }

    @PostMapping("/buy")
    public OrderExecutionResult placeMarketBuy(@RequestBody MarketOrderRequest request) {
        log.info("📥 Получен запрос на покупку: {} - {}, количество: {}",
                request.base(), request.quote(), request.quantity());
        Symbol symbol = new Symbol(request.base(), request.quote());
        OrderExecutionResult result = ordersClient.placeMarketBuy(symbol, request.quantity());
        log.info("✅ Покупка выполнена: {}", result);
        return result;
    }

    @PostMapping("/sell")
    public OrderExecutionResult placeMarketSell(@RequestBody MarketOrderRequest request) {
        log.info("📥 Получен запрос на продажу: {} - {}, количество: {}",
                request.base(), request.quote(), request.quantity());
        Symbol symbol = new Symbol(request.base(), request.quote());
        OrderExecutionResult result = ordersClient.placeMarketSell(symbol, request.quantity());
        log.info("✅ Продажа выполнена: {}", result);
        return result;
    }

    public record MarketOrderRequest(
            String base,
            String quote,
            BigDecimal quantity
    ) {}
}

