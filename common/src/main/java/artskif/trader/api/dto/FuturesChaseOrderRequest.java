package artskif.trader.api.dto;

import java.math.BigDecimal;

/**
 * DTO для запроса на размещение Chase-ордера на фьючерсном рынке.
 * Chase-ордер открывает позицию в указанном направлении,
 * conditional Stop-Loss ордер закрывает её при достижении порогового уровня.
 *
 * @param reduceOnly Если true — Stop-Loss ордер не создаётся, Chase-ордер размещается
 *                   с флагом reduceOnly=true (для закрытия существующей позиции).
 *                   По умолчанию (null или false) — стандартное поведение.
 */
public record FuturesChaseOrderRequest(
        String instrument,
        BigDecimal positionSizeUsdt,
        BigDecimal stopLossPercent,
        Boolean reduceOnly
) {
    /**
     * @return true если reduceOnly явно передан как true, иначе false (null → false).
     */
    public boolean isReduceOnly() {
        return Boolean.TRUE.equals(reduceOnly);
    }
}

