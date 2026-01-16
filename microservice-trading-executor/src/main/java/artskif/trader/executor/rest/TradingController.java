package artskif.trader.executor.rest;

import artskif.trader.executor.orders.positions.ExchangeClient;
import artskif.trader.executor.orders.positions.OrderExecutionResult;
import artskif.trader.executor.orders.model.Symbol;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/trading")
public class TradingController {

    private final ExchangeClient exchangeClient;

    public TradingController(ExchangeClient exchangeClient) {
        this.exchangeClient = exchangeClient;
    }

    @PostMapping("/buy")
    public OrderExecutionResult placeMarketBuy(@RequestBody MarketOrderRequest request) {
        Symbol symbol = new Symbol(request.base(), request.quote());
        return exchangeClient.placeMarketBuy(symbol, request.quantity());
    }

    @PostMapping("/sell")
    public OrderExecutionResult placeMarketSell(@RequestBody MarketOrderRequest request) {
        Symbol symbol = new Symbol(request.base(), request.quote());
        return exchangeClient.placeMarketSell(symbol, request.quantity());
    }

    public record MarketOrderRequest(
            String base,
            String quote,
            BigDecimal quantity
    ) {}
}

