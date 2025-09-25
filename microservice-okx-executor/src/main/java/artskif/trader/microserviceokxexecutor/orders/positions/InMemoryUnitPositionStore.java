package artskif.trader.microserviceokxexecutor.orders.positions;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
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
    public synchronized List<UnitPosition> peekLowestN(int n) {
        if (n <= 0 || queue.isEmpty()) {
            return List.of();
        }
        // Делаем копию min-heap, чтобы не трогать основную очередь
        PriorityQueue<UnitPosition> copy = new PriorityQueue<>(queue);
        int limit = Math.min(n, copy.size());

        List<UnitPosition> result = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            UnitPosition u = copy.poll(); // самый дешёвый с учётом tie-breaker по времени
            if (u == null) break;
            result.add(u);
        }
        return Collections.unmodifiableList(result);
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
