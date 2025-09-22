package artskif.trader.microserviceokxexecutor.orders.positions;

import artskif.trader.microserviceokxexecutor.orders.signal.Side;
import artskif.trader.microserviceokxexecutor.orders.signal.Symbol;

import java.math.BigDecimal;
import java.util.UUID;

/** Инструкция на ОДИН ордер: базовая валюта уже рассчитана стратегией. */
public record OrderInstruction(
        String instructionId,   // генерирует стратегия (для сопоставления на onExecuted)
        Symbol symbol,
        Side side,
        BigDecimal baseQty,     // размер ордера в базовой монете (BTC и т.п.)
        UUID positionRef        // опционально: ссылка на юнит, который закрываем (SELL)
) {
    public static OrderInstruction buy(Symbol symbol, BigDecimal baseQty) {
        return new OrderInstruction(UUID.randomUUID().toString(), symbol, Side.BUY, baseQty, null);
    }
    public static OrderInstruction sell(Symbol symbol, BigDecimal baseQty, UUID positionRef) {
        return new OrderInstruction(UUID.randomUUID().toString(), symbol, Side.SELL, baseQty, positionRef);
    }
}
