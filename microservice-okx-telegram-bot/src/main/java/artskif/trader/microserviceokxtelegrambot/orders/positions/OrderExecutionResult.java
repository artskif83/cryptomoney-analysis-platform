package artskif.trader.microserviceokxtelegrambot.orders.positions;

import java.math.BigDecimal;

public record OrderExecutionResult(
        String exchangeOrderId,
        BigDecimal avgPrice,          // фактическая средняя цена сделки
        BigDecimal executedBaseQty    // фактически исполненный объём базовой монеты
) {}
