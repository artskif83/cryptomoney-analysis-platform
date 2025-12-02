package artskif.trader.microserviceokxexecutor.orders.positions;

import artskif.trader.microserviceokxexecutor.orders.strategy.list.Symbol;
import my.signals.v1.OperationType;

import java.math.BigDecimal;
import java.util.UUID;

/** Инструкция на ОДИН ордер: базовая валюта уже рассчитана стратегией. */
public record OrderInstruction(
        String instructionId,   // генерирует стратегия (для сопоставления на onExecuted)
        Symbol symbol,
        OperationType operationType,
        BigDecimal baseQty,     // размер ордера в базовой монете (BTC и т.п.)
        UUID positionRef        // опционально: ссылка на юнит, который закрываем (SELL)
) {
    public static OrderInstruction buy(Symbol symbol, BigDecimal baseQty) {
        return new OrderInstruction(UUID.randomUUID().toString(), symbol, OperationType.BUY, baseQty, null);
    }
    public static OrderInstruction sell(Symbol symbol, BigDecimal baseQty, UUID positionRef) {
        return new OrderInstruction(UUID.randomUUID().toString(), symbol, OperationType.SELL, baseQty, positionRef);
    }
}
