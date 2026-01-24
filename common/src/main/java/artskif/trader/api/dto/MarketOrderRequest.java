package artskif.trader.api.dto;

import java.math.BigDecimal;

/**
 * DTO для запроса на выполнение рыночного ордера
 */
public record MarketOrderRequest(
        String instrument,
        BigDecimal persentOfDeposit
) {}
