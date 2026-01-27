package artskif.trader.api.dto;

import java.math.BigDecimal;

/**
 * DTO для запроса на выполнение лимитного фьючерсного ордера
 */
public record FuturesLimitOrderRequest(
        String instrument,
        BigDecimal limitPrice,
        BigDecimal positionSizeUsdt,
        BigDecimal stopLossPercent,
        BigDecimal takeProfitPercent
) {}
