package artskif.trader.buffer;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TimeSeriesBufferTest {

    private static final Duration BUCKET = Duration.ofSeconds(60);
    private static final Instant T0 = Instant.parse("2025-01-01T00:00:00Z");

    private Instant bucket(int index) {
        return T0.plus(BUCKET.multipliedBy(index));
    }

    @Test
    void constructorAndInitialState() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(3, BUCKET, "test");

        assertEquals("test", buffer.getName());
        assertEquals(3, buffer.getMaxSize());
        assertTrue(buffer.getDataMap().isEmpty());
        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
        assertNull(buffer.getFirstBucket());
        assertNull(buffer.getLastBucket());
        assertNull(buffer.getFirstItem());
        assertNull(buffer.getLastItem());
        assertEquals(0, buffer.getVersion().get());

        String s = buffer.toString();
        assertTrue(s.contains("itemsCount=0"));
        assertTrue(s.contains("maxSize=3"));
    }

    @Test
    void putItemNewKeyUpdatesCaches() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(10, BUCKET, "test");

        boolean isNew = buffer.putItem(bucket(0), 100);

        assertTrue(isNew);
        assertEquals(1, buffer.size());
        assertFalse(buffer.isEmpty());
        assertEquals(100, buffer.getDataMap().get(bucket(0)));
        assertEquals(bucket(0), buffer.getFirstBucket());
        assertEquals(bucket(0), buffer.getLastBucket());
        assertEquals(100, buffer.getFirstItem());
        assertEquals(100, buffer.getLastItem());
        assertEquals(0, buffer.getVersion().get(), "putItem не должен изменять версию");
    }

    @Test
    void putItemExistingKeyReplacesValue() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(10, BUCKET, "test");

        buffer.putItem(bucket(0), 100);
        boolean isNew = buffer.putItem(bucket(0), 200);

        assertFalse(isNew);
        assertEquals(1, buffer.size());
        assertEquals(200, buffer.getDataMap().get(bucket(0)));
        assertEquals(200, buffer.getFirstItem());
        assertEquals(200, buffer.getLastItem());
    }

    @Test
    void putItemNullIsIgnored() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(10, BUCKET, "test");

        boolean result = buffer.putItem(bucket(0), null);

        assertFalse(result);
        assertEquals(0, buffer.size());
        assertTrue(buffer.isEmpty());
        assertNull(buffer.getFirstBucket());
        assertNull(buffer.getLastBucket());
    }

    @Test
    void putItemMaintainsOrderAndCaches() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(10, BUCKET, "test");

        buffer.putItem(bucket(2), 3);
        buffer.putItem(bucket(0), 1);
        buffer.putItem(bucket(1), 2);

        assertEquals(3, buffer.size());
        assertEquals(bucket(0), buffer.getFirstBucket());
        assertEquals(1, buffer.getFirstItem());
        assertEquals(bucket(2), buffer.getLastBucket());
        assertEquals(3, buffer.getLastItem());
        assertIterableEquals(buffer.getDataMap().keySet(),
                new java.util.TreeSet<>(java.util.Set.of(bucket(0), bucket(1), bucket(2))));
    }

    @Test
    void putItemTrimToMaxSize() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(3, BUCKET, "test");

        buffer.putItem(bucket(0), 0);
        buffer.putItem(bucket(1), 1);
        buffer.putItem(bucket(2), 2);
        buffer.putItem(bucket(3), 3); // должен вызвать trim

        assertEquals(3, buffer.size());
        assertFalse(buffer.getDataMap().containsKey(bucket(0)));
        assertTrue(buffer.getDataMap().containsKey(bucket(1)));
        assertTrue(buffer.getDataMap().containsKey(bucket(2)));
        assertTrue(buffer.getDataMap().containsKey(bucket(3)));

        assertEquals(bucket(1), buffer.getFirstBucket());
        assertEquals(1, buffer.getFirstItem());
        assertEquals(bucket(3), buffer.getLastBucket());
        assertEquals(3, buffer.getLastItem());
    }

    @Test
    void putItemExistingKeyDoesNotIncreaseSizeOrTrim() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(2, BUCKET, "test");

        buffer.putItem(bucket(0), 1);
        buffer.putItem(bucket(1), 2);
        buffer.putItem(bucket(1), 3); // обновление значения

        assertEquals(2, buffer.size());
        assertEquals(1, buffer.getDataMap().get(bucket(0)));
        assertEquals(3, buffer.getDataMap().get(bucket(1)));
        assertEquals(bucket(0), buffer.getFirstBucket());
        assertEquals(bucket(1), buffer.getLastBucket());
    }

    @Test
    void putItemsEmptyMapDoesNothing() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(5, BUCKET, "test");

        buffer.putItems(Collections.emptyMap());

        assertEquals(0, buffer.size());
        assertTrue(buffer.isEmpty());
        assertNull(buffer.getFirstBucket());
        assertNull(buffer.getLastBucket());
        assertEquals(0, buffer.getVersion().get());
    }

    @Test
    void putItemsFillsBufferAndUpdatesCaches() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(5, BUCKET, "test");

        Map<Instant, Integer> data = new HashMap<>();
        data.put(bucket(0), 10);
        data.put(bucket(1), 20);
        data.put(bucket(2), 30);

        buffer.putItems(data);

        assertEquals(3, buffer.size());
        assertEquals(bucket(0), buffer.getFirstBucket());
        assertEquals(10, buffer.getFirstItem());
        assertEquals(bucket(2), buffer.getLastBucket());
        assertEquals(30, buffer.getLastItem());
    }

    @Test
    void putItemsTrimWhenExceedsMaxSize() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(3, BUCKET, "test");

        Map<Instant, Integer> data = new HashMap<>();
        data.put(bucket(0), 0);
        data.put(bucket(1), 1);
        data.put(bucket(2), 2);
        data.put(bucket(3), 3);
        data.put(bucket(4), 4);

        buffer.putItems(data);

        assertEquals(3, buffer.size());
        assertFalse(buffer.getDataMap().containsKey(bucket(0)));
        assertFalse(buffer.getDataMap().containsKey(bucket(1)));
        assertTrue(buffer.getDataMap().containsKey(bucket(2)));
        assertTrue(buffer.getDataMap().containsKey(bucket(3)));
        assertTrue(buffer.getDataMap().containsKey(bucket(4)));

        assertEquals(bucket(2), buffer.getFirstBucket());
        assertEquals(2, buffer.getFirstItem());
        assertEquals(bucket(4), buffer.getLastBucket());
        assertEquals(4, buffer.getLastItem());
    }

    @Test
    void putItemsMergesWithExistingData() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(10, BUCKET, "test");

        buffer.putItem(bucket(0), 1);

        Map<Instant, Integer> data = new HashMap<>();
        data.put(bucket(1), 2);
        data.put(bucket(2), 3);

        buffer.putItems(data);

        assertEquals(3, buffer.size());
        assertTrue(buffer.getDataMap().containsKey(bucket(0)));
        assertTrue(buffer.getDataMap().containsKey(bucket(1)));
        assertTrue(buffer.getDataMap().containsKey(bucket(2)));
        assertEquals(bucket(0), buffer.getFirstBucket());
        assertEquals(bucket(2), buffer.getLastBucket());
    }

    @Test
    void getItemsBetweenVariousCases() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(10, BUCKET, "test");
        for (int i = 0; i < 5; i++) {
            buffer.putItem(bucket(i), i);
        }

        // оба null
        Map<Instant, Integer> all = buffer.getItemsBetween(null, null);
        assertEquals(5, all.size());

        // только after (исключительно)
        Map<Instant, Integer> tail = buffer.getItemsBetween(bucket(1), null);
        assertEquals(3, tail.size());
        assertFalse(tail.containsKey(bucket(1)));

        // только before (включительно)
        Map<Instant, Integer> head = buffer.getItemsBetween(null, bucket(3));
        assertEquals(4, head.size());
        assertTrue(head.containsKey(bucket(3)));

        // оба заданы: (after, before] по реализации
        Map<Instant, Integer> middle = buffer.getItemsBetween(bucket(1), bucket(3));
        assertEquals(2, middle.size());
        assertTrue(middle.containsKey(bucket(2)));
        assertTrue(middle.containsKey(bucket(3)));

        // after == before -> пусто
        Map<Instant, Integer> empty = buffer.getItemsBetween(bucket(2), bucket(2));
        assertTrue(empty.isEmpty());

        // границы вне диапазона
        Map<Instant, Integer> all2 = buffer.getItemsBetween(bucket(-1), bucket(10));
        assertEquals(5, all2.size());

        Map<Instant, Integer> none1 = buffer.getItemsBetween(bucket(10), null);
        assertTrue(none1.isEmpty());

        Map<Instant, Integer> none2 = buffer.getItemsBetween(null, bucket(-1));
        assertTrue(none2.isEmpty());

        // неизменяемость
        Map<Instant, Integer> unmodifiable = buffer.getItemsBetween(null, null);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.put(bucket(0), 999));
    }

    @Test
    void getItemsBetweenOnEmptyBufferReturnsEmptyMap() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(10, BUCKET, "test");
        Map<Instant, Integer> result = buffer.getItemsBetween(null, null);
        assertTrue(result.isEmpty());
    }


    @Test
    void clearResetsStateAndIncrementsVersion() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(10, BUCKET, "test");

        for (int i = 0; i < 3; i++) {
            buffer.putItem(bucket(i), i);
        }

        int versionBefore = buffer.getVersion().get();

        buffer.clear();

        assertTrue(buffer.isEmpty());
        assertEquals(0, buffer.size());
        assertNull(buffer.getFirstBucket());
        assertNull(buffer.getLastBucket());
        assertNull(buffer.getFirstItem());
        assertNull(buffer.getLastItem());
        assertEquals(versionBefore + 1, buffer.getVersion().get());

        int versionAfterFirstClear = buffer.getVersion().get();
        buffer.clear();
        assertEquals(versionAfterFirstClear + 1, buffer.getVersion().get());
    }

    @Test
    void incrementVersionIncreasesVersion() {
        TimeSeriesBuffer<Integer> buffer = new TimeSeriesBuffer<>(10, BUCKET, "test");

        assertEquals(0, buffer.getVersion().get());
        buffer.incrementVersion();
        buffer.incrementVersion();
        assertEquals(2, buffer.getVersion().get());
    }
}

