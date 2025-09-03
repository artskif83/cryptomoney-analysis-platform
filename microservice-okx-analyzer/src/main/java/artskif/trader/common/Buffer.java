package artskif.trader.common;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Buffer<C> {

    @Getter
    protected final Duration interval;
    protected final LimitedLinkedHashMap<Instant, C> writeMap; // только писатель
    // lock-free, упорядочено, неизменяемо
    @Getter
    protected volatile Map<Instant, C> snapshot;               // только читатели

    public Buffer(Duration interval, int maxSize) {
        this.interval = interval;
        this.writeMap = new LimitedLinkedHashMap<>(maxSize); // порядок уже правильный
        this.snapshot = Collections.unmodifiableMap(new LinkedHashMap<>(writeMap));
    }

    // ===== ПУБЛИКАЦИЯ СНИМКА =====


    private void publishSnapshot() {
        // копия до 100 элементов раз в секунду — дёшево
        this.snapshot = Collections.unmodifiableMap(new LinkedHashMap<>(writeMap));
    }

    // ===== API ПИСАТЕЛЯ (один поток) =====

    /** Полная загрузка: данные уже в нужном порядке — просто кладём и публикуем. */
    public void restoreItems(Map<Instant, C> data) {
        writeMap.clear();
        writeMap.putAll(data);     // LimitedLinkedHashMap сам обрежет при переполнении
        publishSnapshot();
    }

    /** Запись новой свечи. Возвращает предыдущее значение (если было). */
    public C putItem(Instant bucket, C item) {
        Instant last = lastBucket();

        if (last != null) {
            boolean same = bucket.equals(last);
            boolean next = bucket.equals(last.plus(interval));
            if (!same && !next) {
                // последовательность нарушена — сбрасываем всё
                System.out.println("📥 [" + interval + "] последовательность свечей нарушена — сбрасываем всё");
                writeMap.clear();
            }
        }

        // если last == null (пусто) — просто начинаем новую последовательность
        C prev = writeMap.put(bucket, item);
        publishSnapshot(); // один раз в секунду — самое то
        return prev;
    }

    /** Возвращает последний bucket в порядке вставки (или null, если пусто). */
    private Instant lastBucket() {
        return writeMap.lastEntry() != null ? writeMap.lastEntry().getKey() : null;
    }

    @Override
    public String toString() {
        return "Buffer{items=" + snapshot.values() + '}';
    }
}
