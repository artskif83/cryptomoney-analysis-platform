package artskif.trader.buffer;

import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeSeriesBuffer<C> {
    @Getter
    private final int maxSize;
    @Getter
    private final ConcurrentSkipListMap<Instant, C> dataMap;

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

    // Объект для синхронизации операций модификации буфера
    private final Object modificationLock = new Object();

    public TimeSeriesBuffer(int maxSize) {
        this.maxSize = maxSize;
        this.dataMap = new ConcurrentSkipListMap<>();
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
    public void putItems(Map<Instant, C> data) {
        if (data.isEmpty()) {
            return;
        }

        synchronized (modificationLock) {
            dataMap.putAll(data);
            trimToSize(); // Удаляем старые элементы если превышен лимит

            // Обновляем lastBucket и lastItem из последнего элемента dataMap
            updateLastBucketAndItem();
            // Обновляем firstBucket и firstItem из первого элемента dataMap
            updateFirstBucketAndItem();
        }
    }

    public boolean putItem(Instant bucket, C item) {
        if (item == null) {
            return false;
        }

        synchronized (modificationLock) {
            C inserted = dataMap.put(bucket, item);
            trimToSize(); // Удаляем старые элементы если превышен лимит

            // Обновляем lastBucket и lastItem из последнего элемента dataMap
            updateLastBucketAndItem();
            // Обновляем firstBucket и firstItem из первого элемента dataMap
            updateFirstBucketAndItem();

            return inserted == null;
        }
    }

    /**
     * Проверяет существование элемента с указанной временной меткой в буфере.
     *
     * @param bucket временная метка для проверки
     * @return true если элемент с указанной временной меткой существует в буфере, false иначе
     */
    public boolean containsKey(Instant bucket) {
        return dataMap.containsKey(bucket);
    }

    /**
     * Возвращает элементы из буфера между указанными временными метками.
     * Использует эффективные методы ConcurrentSkipListMap (subMap, tailMap, headMap) для получения подмножества за O(log n).
     * Границы интервала не включаются в результат.
     *
     * @param after  начальная граница интервала (не включается в результат), может быть null для выборки с начала
     * @param before конечная граница интервала (не включается в результат), может быть null для выборки до конца
     * @return неизменяемая map с элементами в хронологическом порядке, пустая map если нет подходящих элементов
     */
    public Map<Instant, C> getItemsBetween(Instant after, Instant before) {
        // Получаем подмножество используя эффективные методы ConcurrentSkipListMap
        ConcurrentSkipListMap<Instant, C> subMap;

        if (after != null && before != null) {
            // Оба параметра заданы: используем subMap с исключающими границами
            subMap = new ConcurrentSkipListMap<>(dataMap.subMap(after, false, before, true));
        } else if (after != null) {
            // Только after задан: используем tailMap с исключающей нижней границей
            subMap = new ConcurrentSkipListMap<>(dataMap.tailMap(after, false));
        } else if (before != null) {
            // Только before задан: используем headMap с исключающей верхней границей
            subMap = new ConcurrentSkipListMap<>(dataMap.headMap(before, true));
        } else {
            // Оба параметра null: возвращаем все элементы
            subMap = new ConcurrentSkipListMap<>(dataMap);
        }

        return Collections.unmodifiableMap(subMap);
    }



    /**
     * Полностью очищает буфер, удаляя все элементы и сбрасывая кэшированные значения.
     * После вызова этого метода буфер будет пустым, lastBucket и lastItem будут установлены в null.
     * Метод потокобезопасен и синхронизирован с другими операциями модификации буфера.
     */
    public void clear() {
        synchronized (modificationLock) {
            dataMap.clear();
            lastBucket = null;
            lastItem = null;
            firstBucket = null;
            firstItem = null;
            incrementVersion();
        }
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

