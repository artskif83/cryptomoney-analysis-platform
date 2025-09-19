package artskif.trader.microserviceokxtelegrambot.orders.strategy;

import artskif.trader.microserviceokxtelegrambot.orders.positions.OrderExecutionResult;
import artskif.trader.microserviceokxtelegrambot.orders.positions.OrderInstruction;
import artskif.trader.microserviceokxtelegrambot.orders.signal.Signal2;
import my.signals.v1.StrategyKind;

import java.util.List;

public interface Strategy {
    boolean supports(StrategyKind kind);

    /** Возвращает один или несколько ордеров, которые нужно выполнить сейчас. */
    List<OrderInstruction> decide(Signal2 signal);

    /** Коллбек об исполнении конкретной инструкции (для обновления стора внутри стратегии). */
    void onExecuted(OrderInstruction instruction, OrderExecutionResult fill);
}
