package artskif.trader.common;

import java.util.LinkedHashMap;
import java.util.Map;

public class LimitedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public LimitedLinkedHashMap(int maxSize) {
        super(128, 0.75f, true); // true = access-order, можно заменить на false
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
