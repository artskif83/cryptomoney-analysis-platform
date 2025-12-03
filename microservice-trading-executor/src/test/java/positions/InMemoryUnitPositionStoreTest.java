package positions;

import artskif.trader.executor.orders.positions.InMemoryUnitPositionStore;
import artskif.trader.executor.orders.positions.UnitPositionStore;
import artskif.trader.executor.orders.strategy.list.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUnitPositionStoreTest {

    private InMemoryUnitPositionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryUnitPositionStore();
    }

    // ---------- helpers ----------

    private static UnitPositionStore.UnitPosition u(UUID id, double price, Instant ts) {
        return new UnitPositionStore.UnitPosition(id, new Symbol("bTC", "USDT"), BigDecimal.valueOf(price), BigDecimal.valueOf(0.01), ts);
    }

    // ---------- tests ----------

    @Test
    void add_and_usedUnits_and_peekLowest_basicOrdering() {
        UnitPositionStore.UnitPosition a = u(UUID.randomUUID(), 10.0, Instant.parse("2025-01-01T00:00:01Z"));
        UnitPositionStore.UnitPosition b = u(UUID.randomUUID(),  9.5, Instant.parse("2025-01-01T00:00:02Z"));
        UnitPositionStore.UnitPosition c = u(UUID.randomUUID(), 12.0, Instant.parse("2025-01-01T00:00:03Z"));

        store.add(a);
        store.add(b);
        store.add(c);

        assertEquals(3, store.usedUnits());

        Optional<UnitPositionStore.UnitPosition> lowest = store.peekLowest();
        assertTrue(lowest.isPresent());
        // самый дешёвый — b (9.5)
        assertSame(b, lowest.get());
    }

    @Test
    void ordering_byPrice_thenByTime_tieBreaker() {
        Instant t1 = Instant.parse("2025-01-01T00:00:01Z");
        Instant t2 = Instant.parse("2025-01-01T00:00:02Z");
        // Одинаковая цена, разное время — приоритет более раннего
        UnitPositionStore.UnitPosition early = u(UUID.randomUUID(), 10.0, t1);
        UnitPositionStore.UnitPosition later = u(UUID.randomUUID(), 10.0, t2);
        UnitPositionStore.UnitPosition cheaper = u(UUID.randomUUID(),  9.9, t2);

        store.add(later);
        store.add(early);
        store.add(cheaper);

        // самый дешёвый по цене
        assertSame(cheaper, store.peekLowest().orElseThrow());
        // убираем его и проверяем tie-breaker по времени
        assertSame(cheaper, store.pollLowest().orElseThrow());
        assertSame(early, store.peekLowest().orElseThrow());
    }

    @Test
    void peekLowestN_returnsSorted_and_doesNotMutateStore() {
        List<UnitPositionStore.UnitPosition> inserted = new ArrayList<>();
        inserted.add(u(UUID.randomUUID(), 11.0, Instant.parse("2025-01-01T00:00:03Z")));
        inserted.add(u(UUID.randomUUID(),  9.0, Instant.parse("2025-01-01T00:00:01Z")));
        inserted.add(u(UUID.randomUUID(), 10.0, Instant.parse("2025-01-01T00:00:02Z")));
        inserted.add(u(UUID.randomUUID(), 12.0, Instant.parse("2025-01-01T00:00:04Z")));
        inserted.add(u(UUID.randomUUID(),  8.5, Instant.parse("2025-01-01T00:00:00Z")));

        inserted.forEach(store::add);

        List<UnitPositionStore.UnitPosition> top3 = store.peekLowestN(3);
        assertEquals(3, top3.size(), "peekLowestN should return exactly N elements when enough items present");

        // проверим возрастание по цене/времени
        List<UnitPositionStore.UnitPosition> sorted =
                top3.stream().collect(Collectors.toList()); // copy for clarity

        // Сравним с эталонной сортировкой того же набора (без мутации стора)
        List<UnitPositionStore.UnitPosition> snapshotSorted = store.snapshot().stream()
                .sorted(Comparator
                        .comparing((UnitPositionStore.UnitPosition u) -> u.purchasePrice)
                        .thenComparing(u -> u.purchasedAt))
                .limit(3)
                .collect(Collectors.toList());

        assertEquals(snapshotSorted, sorted, "peekLowestN must follow the same ordering as the internal min-heap");

        // Хранилище не должно измениться
        assertEquals(5, store.usedUnits(), "peekLowestN must not mutate the store");
        assertEquals(snapshotSorted.get(0), store.peekLowest().orElseThrow());
    }

    @Test
    void peekLowestN_edgeCases_empty_zero_negative_moreThanSize() {
        assertTrue(store.peekLowestN(3).isEmpty(), "empty store => empty list");
        assertTrue(store.peekLowestN(0).isEmpty(), "n=0 => empty list");
        assertTrue(store.peekLowestN(-1).isEmpty(), "n<0 => empty list");

        store.add(u(UUID.randomUUID(), 5.0, Instant.parse("2025-01-01T00:00:01Z")));
        store.add(u(UUID.randomUUID(), 6.0, Instant.parse("2025-01-01T00:00:02Z")));

        List<UnitPositionStore.UnitPosition> all = store.peekLowestN(10);
        assertEquals(2, all.size(), "when n > size, return all");
    }

    @Test
    void pollLowest_removesInCorrectOrder_untilEmpty() {
        UnitPositionStore.UnitPosition a = u(UUID.randomUUID(), 7.0, Instant.parse("2025-01-01T00:00:03Z"));
        UnitPositionStore.UnitPosition b = u(UUID.randomUUID(), 5.0, Instant.parse("2025-01-01T00:00:01Z"));
        UnitPositionStore.UnitPosition c = u(UUID.randomUUID(), 6.0, Instant.parse("2025-01-01T00:00:02Z"));

        store.add(a);
        store.add(b);
        store.add(c);

        assertSame(b, store.pollLowest().orElseThrow());
        assertSame(c, store.pollLowest().orElseThrow());
        assertSame(a, store.pollLowest().orElseThrow());
        assertTrue(store.pollLowest().isEmpty(), "after removing all, pollLowest returns empty");
        assertEquals(0, store.usedUnits());
    }

    @Test
    void findById_and_removeById() {
        UUID idKeep = UUID.randomUUID();
        UUID idDrop = UUID.randomUUID();
        UnitPositionStore.UnitPosition keep = u(idKeep, 9.0, Instant.parse("2025-01-01T00:00:01Z"));
        UnitPositionStore.UnitPosition drop = u(idDrop, 8.0, Instant.parse("2025-01-01T00:00:02Z"));

        store.add(keep);
        store.add(drop);

        assertTrue(store.findById(idDrop).isPresent());
        assertTrue(store.removeById(idDrop), "removeById must return true for existing element");
        assertTrue(store.findById(idDrop).isEmpty(), "removed element should not be found");
        assertEquals(1, store.usedUnits());
        assertSame(keep, store.peekLowest().orElseThrow());
        assertFalse(store.removeById(UUID.randomUUID()), "removeById for unknown id => false");
    }

    @Test
    void snapshot_returnsCopy_notLinkedToInternalPQ() {
        UnitPositionStore.UnitPosition a = u(UUID.randomUUID(), 1.0, Instant.parse("2025-01-01T00:00:01Z"));
        UnitPositionStore.UnitPosition b = u(UUID.randomUUID(), 2.0, Instant.parse("2025-01-01T00:00:02Z"));
        store.add(a);
        store.add(b);

        List<UnitPositionStore.UnitPosition> snap = store.snapshot();
        assertEquals(2, snap.size());

        // Модификация снапа не должна влиять на стор
        snap.clear();
        assertEquals(2, store.usedUnits(), "snapshot must be a detached copy");

        // После clear() стора — снап остаётся прежним (проверим косвенно)
        store.clear();
        assertEquals(0, store.usedUnits());
        assertEquals(0, store.snapshot().size());
    }

    @Test
    void clear_emptiesStore() {
        store.add(u(UUID.randomUUID(), 3.0, Instant.parse("2025-01-01T00:00:01Z")));
        store.add(u(UUID.randomUUID(), 4.0, Instant.parse("2025-01-01T00:00:02Z")));
        assertEquals(2, store.usedUnits());
        store.clear();
        assertEquals(0, store.usedUnits());
        assertTrue(store.peekLowest().isEmpty());
    }
}
