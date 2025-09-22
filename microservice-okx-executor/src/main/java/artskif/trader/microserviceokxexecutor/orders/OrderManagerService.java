package artskif.trader.microserviceokxexecutor.orders;


import artskif.trader.microserviceokxexecutor.orders.positions.ExchangeClient;
import artskif.trader.microserviceokxexecutor.orders.positions.OrderExecutionResult;
import artskif.trader.microserviceokxexecutor.orders.positions.OrderInstruction;
import artskif.trader.microserviceokxexecutor.orders.strategy.Strategy;
import artskif.trader.microserviceokxexecutor.orders.strategy.StrategyRegistry;
import artskif.trader.microserviceokxexecutor.orders.strategy.list.Symbol;
import my.signals.v1.OperationType;
import my.signals.v1.Signal;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class OrderManagerService {

    private final StrategyRegistry registry;
    private final ExchangeClient exchange;
    private final ConcurrentHashMap<String, ReentrantLock> symbolLocks = new ConcurrentHashMap<>();

    public OrderManagerService(StrategyRegistry registry, ExchangeClient exchange) {
        this.registry = registry;
        this.exchange = exchange;
    }

    public void onSignal(Signal signal) {
        Strategy strategy = registry.resolve(signal.getStrategy());
        List<OrderInstruction> instructions = strategy.decide(signal);
        if (instructions.isEmpty()) return;

        var lock = symbolLocks.computeIfAbsent(Symbol.fromProto(signal.getSymbol()).asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            for (OrderInstruction instr : instructions) {
                OrderExecutionResult fill = (instr.operationType() == OperationType.BUY)
                        ? exchange.placeMarketBuy(instr.symbol(), instr.baseQty())
                        : exchange.placeMarketSell(instr.symbol(), instr.baseQty());

                strategy.onExecuted(instr, fill);
            }
        } finally {
            lock.unlock();
        }
    }
}
