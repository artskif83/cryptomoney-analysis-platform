package artskif.trader.microserviceokxtelegrambot.orders;


import artskif.trader.microserviceokxtelegrambot.orders.positions.ExchangeClient;
import artskif.trader.microserviceokxtelegrambot.orders.positions.OrderExecutionResult;
import artskif.trader.microserviceokxtelegrambot.orders.positions.OrderInstruction;
import artskif.trader.microserviceokxtelegrambot.orders.signal.Side;
import artskif.trader.microserviceokxtelegrambot.orders.signal.Signal2;
import artskif.trader.microserviceokxtelegrambot.orders.strategy.Strategy;
import artskif.trader.microserviceokxtelegrambot.orders.strategy.StrategyRegistry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public void onSignal(Signal2 signal) {
        Strategy strategy = registry.resolve(signal.strategyKind());
        List<OrderInstruction> instructions = strategy.decide(signal);
        if (instructions.isEmpty()) return;

        var lock = symbolLocks.computeIfAbsent(signal.symbol().asPair(), k -> new ReentrantLock());
        lock.lock();
        try {
            for (OrderInstruction instr : instructions) {
                OrderExecutionResult fill = (instr.side() == Side.BUY)
                        ? exchange.placeMarketBuy(instr.symbol(), instr.baseQty())
                        : exchange.placeMarketSell(instr.symbol(), instr.baseQty());

                strategy.onExecuted(instr, fill);
            }
        } finally {
            lock.unlock();
        }
    }
}
