package artskif.trader.api.dto;

import java.math.BigDecimal;

/**
 * DTO для запроса на размещение Chase-ордера на фьючерсном рынке.
 * Chase-ордер открывает позицию в указанном направлении,
 * conditional Stop-Loss ордер закрывает её при достижении порогового уровня.
 */
public record FuturesChaseOrderRequest(
        String instrument,
        BigDecimal positionSizeUsdt,
        BigDecimal stopLossPercent
) {}

