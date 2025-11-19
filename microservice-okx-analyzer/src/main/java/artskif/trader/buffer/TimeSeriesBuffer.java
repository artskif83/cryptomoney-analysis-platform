package artskif.trader.buffer;

import lombok.Getter;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
public class TimeSeriesBuffer<C> {

    private final static Logger LOG = Logger.getLogger(TimeSeriesBuffer.class);
    private static final int EXTRA_CHECK_COUNT = 300; // Дополнительные элементы для проверки
    private final Duration bucketDuration;
    private final int maxSize;

    @Getter
    protected final ConcurrentSkipListMap<Instant, C> dataMap;

    @Getter
    private volatile Instant lastBucket = null;
    @Getter
    private volatile C lastItem = null;
    @Getter
    private volatile Instant firstBucket = null;
    @Getter
    private volatile C firstItem = null;
    @Getter
    private final AtomicInteger version;

    public TimeSeriesBuffer(int maxSize, Duration bucketDuration) {
        this.maxSize = maxSize;
        this.dataMap = new ConcurrentSkipListMap<>();
        this.bucketDuration = bucketDuration;
        this.version = new AtomicInteger(0);
    }

    public void incrementVersion() {
        this.version.incrementAndGet();
    }

    /**
     * Удаляет самые старые элементы из буфера, если текущий размер превышает максимально допустимый.
     * Использует метод pollFirstEntry() из ConcurrentSkipListMap для эффективного удаления первых элементов.
     */
    private void trimToSize() {
        boolean removed = false;
        while (dataMap.size() > maxSize) {
            dataMap.pollFirstEntry();
            removed = true;
        }
        if (removed) {
            updateFirstBucketAndItem();
        }
    }

    /**
     * Обновляет кэшированные значения lastBucket и lastItem на основе последнего элемента в буфере.
     * Использует эффективный метод lastEntry() из ConcurrentSkipListMap для получения последнего элемента за O(log n).
     * Если буфер пуст, устанавливает оба значения в null.
     */
    private void updateLastBucketAndItem() {
        Map.Entry<Instant, C> lastEntry = dataMap.lastEntry();
        if (lastEntry != null) {
            lastBucket = lastEntry.getKey();
            lastItem = lastEntry.getValue();
        } else {
            lastBucket = null;
            lastItem = null;
        }
    }

    /**
     * Обновляет кэшированные значения firstBucket и firstItem на основе первого элемента в буфере.
     * Использует эффективный метод firstEntry() из ConcurrentSkipListMap для получения первого элемента за O(log n).
     * Если буфер пуст, устанавливает оба значения в null.
     */
    private void updateFirstBucketAndItem() {
        Map.Entry<Instant, C> firstEntry = dataMap.firstEntry();
        if (firstEntry != null) {
            firstBucket = firstEntry.getKey();
            firstItem = firstEntry.getValue();
        } else {
            firstBucket = null;
            firstItem = null;
        }
    }

    /**
     * Проверяет непрерывность временных интервалов в последних элементах буфера.
     * Все соседние bucket'ы должны отличаться друг от друга ровно на bucketDuration.
     * При обнаружении разрывов (пропущенных временных интервалов) выводит предупреждение в лог.
     * Проверка помогает выявлять проблемы с поступлением данных.
     *
     * @param lastCount количество последних элементов для проверки непрерывности
     */
    private void checkContinuity(int lastCount) {
        if (dataMap.size() < 2) {
            return; // Нечего проверять
        }

        // Получаем последние lastCount элементов
        int checkSize = Math.min(lastCount, dataMap.size());
        Instant[] buckets = new Instant[checkSize];
        int index = 0;
        int skip = dataMap.size() - checkSize;
        int current = 0;

        for (Instant bucket : dataMap.keySet()) {
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
//                LOG.warnf("Обнаружен разрыв в непрерывности данных: prevBucket=%s, currBucket=%s, " +
//                                "gap=%s, expect=%s (index=%d из %d проверяемых)",
//                        prev, curr, gap, bucketDuration, i, buckets.length);
            }
        }
    }

    /**
     * Проверяет, пуст ли буфер временных рядов.
     *
     * @return true если в буфере нет элементов, false иначе
     */
    public boolean isEmpty() {
        return dataMap.isEmpty();
    }

    /**
     * Возвращает текущее количество элементов в буфере.
     *
     * @return количество элементов в буфере
     */
    public Integer size() {
        return dataMap.size();
    }

    /**
     * Массово загружает элементы в буфер из переданной map.
     * Используется для восстановления состояния буфера из внешнего источника данных.
     * После загрузки обновляет кэшированные значения и проверяет непрерывность временных рядов.
     * Если размер превышает максимально допустимый, автоматически удаляет самые старые элементы.
     *
     * @param data map с данными для загрузки, где ключ - временная метка bucket, значение - элемент данных
     */
    public void restoreItems(Map<Instant, C> data) {
        if (data.isEmpty()) {
            return;
        }

        dataMap.putAll(data);
        trimToSize(); // Удаляем старые элементы если превышен лимит

        // Обновляем lastBucket и lastItem из последнего элемента dataMap
        updateLastBucketAndItem();
        // Обновляем firstBucket и firstItem из первого элемента dataMap
        updateFirstBucketAndItem();

        // Проверяем непрерывность: количество добавленных элементов + дополнительные
        checkContinuity(data.size() + EXTRA_CHECK_COUNT);
    }

    /**
     * Добавляет новый элемент в буфер по указанной временной метке bucket.
     * Если элемент с такой меткой уже существует, он будет заменён.
     * После добавления обновляет кэшированные значения lastBucket и lastItem,
     * удаляет старые элементы при превышении maxSize и проверяет непрерывность временных рядов.
     *
     * @param bucket временная метка для элемента
     * @param item элемент данных для добавления
     * @return предыдущее значение элемента с такой же временной меткой или null, если его не было
     */
    public C putItem(Instant bucket, C item) {
        C inserted = dataMap.put(bucket, item);
        trimToSize(); // Удаляем старые элементы если превышен лимит

        // Обновляем lastBucket и lastItem из последнего элемента dataMap
        updateLastBucketAndItem();
        // Обновляем firstBucket и firstItem из первого элемента dataMap
        updateFirstBucketAndItem();

        // Проверяем непрерывность: 1 добавленный элемент + дополнительные
        checkContinuity(1 + EXTRA_CHECK_COUNT);

        return inserted;
    }

    /**
     * Возвращает элементы из буфера между указанными временными метками.
     * Использует эффективные методы ConcurrentSkipListMap (subMap, tailMap, headMap) для получения подмножества за O(log n).
     * Границы интервала не включаются в результат.
     *
     * @param after начальная граница интервала (не включается в результат), может быть null для выборки с начала
     * @param before конечная граница интервала (не включается в результат), может быть null для выборки до конца
     * @return неизменяемая map с элементами в хронологическом порядке, пустая map если нет подходящих элементов
     */
    public Map<Instant, C> getItemsBetween(Instant after, Instant before) {
        // Получаем подмножество используя эффективные методы ConcurrentSkipListMap
        ConcurrentSkipListMap<Instant, C> subMap;

        if (after != null && before != null) {
            // Оба параметра заданы: используем subMap с исключающими границами
            subMap = new ConcurrentSkipListMap<>(dataMap.subMap(after, false, before, false));
        } else if (after != null) {
            // Только after задан: используем tailMap с исключающей нижней границей
            subMap = new ConcurrentSkipListMap<>(dataMap.tailMap(after, false));
        } else if (before != null) {
            // Только before задан: используем headMap с исключающей верхней границей
            subMap = new ConcurrentSkipListMap<>(dataMap.headMap(before, false));
        } else {
            // Оба параметра null: возвращаем все элементы
            subMap = new ConcurrentSkipListMap<>(dataMap);
        }

        return Collections.unmodifiableMap(subMap);
    }

    /**
     * Полностью очищает буфер, удаляя все элементы и сбрасывая кэшированные значения.
     * После вызова этого метода буфер будет пустым, lastBucket и lastItem будут установлены в null.
     */
    public void clear() {
        dataMap.clear();
        lastBucket = null;
        lastItem = null;
        firstBucket = null;
        firstItem = null;
    }

    @Override
    public String toString() {
        return "Buffer{itemsCount=" + dataMap.size() +
                       ", maxSize=" + maxSize +
                       ", firstBucket=" + firstBucket +
                       ", lastBucket=" + lastBucket +
                       ", lastItem=" + lastItem + '}';
    }
}
