package artskif.trader.microserviceokxexecutor.orders.strategy;

import artskif.trader.microserviceokxexecutor.orders.positions.OrderExecutionResult;
import artskif.trader.microserviceokxexecutor.orders.positions.OrderInstruction;
import my.signals.v1.Signal;
import my.signals.v1.StrategyKind;

import java.util.List;

public interface Strategy {
    boolean supports(StrategyKind kind);

    /** Возвращает один или несколько ордеров, которые нужно выполнить сейчас. */
    List<OrderInstruction> decide(Signal signal);

    /** Коллбек об исполнении конкретной инструкции (для обновления стора внутри стратегии). */
    void onExecuted(OrderInstruction instruction, OrderExecutionResult fill);
}
