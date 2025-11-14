package artskif.trader.buffer;

import lombok.Getter;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TimeSeriesBuffer<C> {

    private final static Logger LOG = Logger.getLogger(TimeSeriesBuffer.class);
    private static final int EXTRA_CHECK_COUNT = 10; // Дополнительные элементы для проверки
    private static final int BATCH_SIZE = 300; // Размер партии для восстановления
    private final Duration bucketDuration;
    @Getter
    protected final LimitedLinkedHashMap<Instant, C> writeMap; // только писатель

    // lock-free, упорядочено, неизменяемо
    @Getter
    protected volatile Map<Instant, C> snapshot; // только читатели
    @Getter
    private volatile Instant lastBucket = null;
    @Getter
    private volatile C lastItem = null;

    public TimeSeriesBuffer(int maxSize, Duration bucketDuration) {
        this.writeMap = new LimitedLinkedHashMap<>(maxSize); // порядок уже правильный
        this.snapshot = Collections.unmodifiableMap(new LinkedHashMap<>(writeMap));
        this.bucketDuration = bucketDuration;
    }

    // ===== ПУБЛИКАЦИЯ СНИМКА =====
    private void publishSnapshot() {
        this.snapshot = Collections.unmodifiableMap(new LinkedHashMap<>(writeMap));
    }

    /**
     * Проверяет непрерывность последних lastCount элементов буфера.
     * Все бакеты должны отличаться друг от друга ровно на bucketDuration.
     * При обнаружении разрывов выводит предупреждение.
     *
     * @param lastCount количество последних элементов для проверки
     */
    private void checkContinuity(int lastCount) {
        if (writeMap.size() < 2) {
            return; // Нечего проверять
        }

        // Получаем последние lastCount элементов
        int checkSize = Math.min(lastCount, writeMap.size());
        Instant[] buckets = new Instant[checkSize];
        int index = 0;
        int skip = writeMap.size() - checkSize;
        int current = 0;

        for (Instant bucket : writeMap.keySet()) {
            if (current >= skip) {
                buckets[index++] = bucket;
            }
            current++;
        }

        // Проверяем непрерывность
        for (int i = 1; i < buckets.length; i++) {
            Instant prev = buckets[i - 1];
            Instant curr = buckets[i];
            Duration gap = Duration.between(prev, curr);

            if (!gap.equals(bucketDuration)) {
                LOG.warnf("Обнаружен разрыв в непрерывности данных: prevBucket=%s, currBucket=%s, " +
                                "gap=%s, expect=%s (index=%d из %d проверяемых)",
                        prev, curr, gap, bucketDuration, i, buckets.length);
            }
        }
    }

    // ===== API ПИСАТЕЛЯ (один поток) =====

    /**
     * Проверяет, пуст ли буфер.
     */
    public boolean isEmpty() {
        return snapshot.isEmpty();
    }

    /**
     * Полная загрузка: данные уже в нужном порядке — просто кладём и публикуем.
     */
    public void restoreItems(Map<Instant, C> data) {
        if (data.isEmpty()) {
            return;
        }

        // Проверяем, что новые данные корректно соединяются с последним бакетом
        if (lastBucket != null) {
            Instant firstNewBucket = data.keySet().iterator().next();
            Duration gap = Duration.between(lastBucket, firstNewBucket);

            if (gap.isNegative() || gap.compareTo(bucketDuration) > 0) {
                LOG.warnf("Элементы не добавлены(restoreItems). Нарушена непрерывность данных: lastBucket=%s, firstNewBucket=%s, gap=%s, maxAllowed=%s",
                        lastBucket, firstNewBucket, gap, bucketDuration);
                return;
            }
        }

        writeMap.putAll(data);     // LimitedLinkedHashMap сам обрежет при переполнении

        // Обновляем lastBucket и lastItem на последний элемент из новых данных
        Instant lastKey = null;
        C lastValue = null;
        for (Map.Entry<Instant, C> entry : data.entrySet()) {
            lastKey = entry.getKey();
            lastValue = entry.getValue();
        }
        if (lastKey != null) {
            lastBucket = lastKey;
            lastItem = lastValue;
        }

        publishSnapshot();

        // Проверяем непрерывность: количество добавленных элементов + дополнительные
        checkContinuity(data.size() + EXTRA_CHECK_COUNT);
    }

    /**
     * Запись новой свечи. Возвращает предыдущее значение (если было).
     */
    public C putItem(Instant bucket, C item) {
        // Проверяем, что новый бакет корректно соединяется с последним бакетом
        if (lastBucket != null) {
            Duration gap = Duration.between(lastBucket, bucket);

            if (gap.isNegative() || gap.compareTo(bucketDuration) > 0) {
                LOG.warnf("Элемент не добавлен(putItem). Нарушена непрерывность данных: lastBucket=%s, newBucket=%s, gap=%s, maxAllowed=%s",
                        lastBucket, bucket, gap, bucketDuration);
                return null;
            }
        }

        C inserted = writeMap.put(bucket, item);

        // Обновляем lastBucket если новый бакет позже текущего
        if (lastBucket == null || bucket.isAfter(lastBucket)) {
            lastBucket = bucket;
            lastItem = item;
        }

        publishSnapshot(); // Публикуем новый снимок

        // Проверяем непрерывность: 1 добавленный элемент + дополнительные
        checkContinuity(1 + EXTRA_CHECK_COUNT);

        return inserted;
    }

    /**
     * Возвращает элементы младше указанного bucket, но не более BATCH_SIZE штук.
     * Если bucket равен null, возвращает элементы с начала списка.
     *
     * @param bucket граничный bucket (не включается в результат), может быть null
     * @return неизменяемая map с элементами, отсортированная по времени
     */
    public Map<Instant, C> getItemsAfter(Instant bucket) {
        Map<Instant, C> result = new LinkedHashMap<>();

        for (Map.Entry<Instant, C> entry : snapshot.entrySet()) {
            if (bucket == null || entry.getKey().isAfter(bucket)) {
                result.put(entry.getKey(), entry.getValue());

                if (result.size() >= BATCH_SIZE) {
                    break;
                }
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Очистка буфера.
     */
    public void clear() {
        writeMap.clear();
        publishSnapshot();
        lastBucket = null;
        lastItem = null;
    }

    @Override
    public String toString() {
        return "Buffer{itemsCount=" + snapshot.values().size() + '}';
    }
}
