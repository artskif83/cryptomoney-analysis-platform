package artskif.trader.api.dto;

import java.math.BigDecimal;

/**
 * DTO для запроса на выполнение рыночного ордера
 */
public record MarketOrderRequest(
        String base,
        String quote,
        BigDecimal quantity
) {}
