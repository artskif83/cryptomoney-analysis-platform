package artskif.trader.microserviceokxexecutor.orders.positions;

import java.util.*;


public final class InMemoryUnitPositionStore implements UnitPositionStore {

    // Мин-куча по цене покупки; при равенстве — по времени
    private final PriorityQueue<UnitPosition> queue = new PriorityQueue<>(
            Comparator
                    .comparing((UnitPosition u) -> u.purchasePrice)
                    .thenComparing(u -> u.purchasedAt)
    );

    @Override
    public synchronized void add(UnitPosition unit) {
        queue.add(unit);
    }

    @Override
    public synchronized Optional<UnitPosition> peekLowest() {
        return Optional.ofNullable(queue.peek());
    }

    @Override
    public synchronized Optional<UnitPosition> pollLowest() {
        return Optional.ofNullable(queue.poll());
    }

    @Override
    public synchronized Optional<UnitPosition> findById(java.util.UUID id) {
        for (UnitPosition u : queue) {
            if (u.id.equals(id)) return Optional.of(u);
        }
        return Optional.empty();
    }

    @Override
    public synchronized boolean removeById(java.util.UUID id) {
        final Iterator<UnitPosition> it = queue.iterator();
        while (it.hasNext()) {
            if (it.next().id.equals(id)) {
                it.remove();          // корректно удаляет из PriorityQueue
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized int usedUnits() {
        return queue.size();
    }

    @Override
    public synchronized java.util.List<UnitPosition> snapshot() {
        return new java.util.ArrayList<>(queue); // копия
    }

    @Override
    public synchronized void clear() {
        queue.clear();
    }
}
