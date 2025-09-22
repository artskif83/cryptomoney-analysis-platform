package artskif.trader.microserviceokxexecutor.orders.strategy;

import my.signals.v1.StrategyKind;

import java.util.List;

public final class StrategyRegistry {
    private final List<Strategy> strategies;

    public StrategyRegistry(List<Strategy> strategies) {
        this.strategies = strategies;
    }

    public Strategy resolve(StrategyKind kind) {
        return strategies.stream()
                .filter(s -> s.supports(kind))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No strategy for: " + kind));
    }
}
