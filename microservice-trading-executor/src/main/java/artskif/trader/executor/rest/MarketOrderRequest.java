package artskif.trader.executor.rest;

import java.math.BigDecimal;

public record MarketOrderRequest(
        String base,
        String quote,
        BigDecimal quantity
) {}

