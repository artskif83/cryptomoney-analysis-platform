package artskif.trader.buffer;

import lombok.Getter;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Buffer<C> {

    @Getter
    protected final LimitedLinkedHashMap<Instant, C> writeMap; // только писатель
    // lock-free, упорядочено, неизменяемо
    @Getter
    protected volatile Map<Instant, C> snapshot; // только читатели
    @Getter
    private volatile long version = 0L;

    public Buffer(int maxSize) {
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
        writeMap.putAll(data);     // LimitedLinkedHashMap сам обрежет при переполнении
        publishSnapshot();
        version++;
    }

    /** Запись новой свечи. Возвращает предыдущее значение (если было). */
    public C putItem(Instant bucket, C item) {
        C inserted = writeMap.put(bucket, item);
        publishSnapshot(); // один раз в секунду — самое то
        return inserted;
    }

    @Override
    public String toString() {
        return "Buffer{items=" + snapshot.values() + '}';
    }
}
