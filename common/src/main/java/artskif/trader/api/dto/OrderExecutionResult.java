package artskif.trader.api.dto;

import java.math.BigDecimal;

/**
 * DTO с результатом выполнения ордера
 */
public record OrderExecutionResult(
        String exchangeOrderId,
        BigDecimal avgPrice,          // фактическая средняя цена сделки
        BigDecimal executedBaseQty    // фактически исполненный объём базовой монеты
) {}

