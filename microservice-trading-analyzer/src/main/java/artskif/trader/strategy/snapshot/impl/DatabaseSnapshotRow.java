package artskif.trader.strategy.snapshot.impl;

import artskif.trader.strategy.snapshot.DatabaseSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class DatabaseSnapshotRow implements DatabaseSnapshot {

    private final Duration timeframe;
    private final Instant timestamp;
    private final String contractHash;
    private final String tag;
    private final Map<String, Object> columns;

    public DatabaseSnapshotRow(Duration timeframe, Instant timestamp,
                               String contractHash, String tag) {
        this.timeframe = timeframe;
        this.timestamp = timestamp;
        this.contractHash = contractHash;
        this.tag = tag;
        this.columns = new HashMap<>();
    }

    /**
     * Добавить фичу в строку
     */
    public void addColumn(String columnName, Object value) {
        columns.put(columnName, value);
    }

    /**
     * Получить значение фичи
     */
    public Object getColumnValue(String columnName) {
        return columns.get(columnName);
    }

    /**
     * Получить все фичи
     */
    public Map<String, Object> getAllColumns() {
        return new HashMap<>(columns);
    }

    @Override
    public Duration getTimeframe() {
        return timeframe;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String contractHash() {
        return contractHash;
    }

    @Override
    public String tag() {
        return tag;
    }

    @Override
    public String toString() {
        return String.format("DatabaseRow{tf=%s, ts=%s, hash='%s', tag='%s', columns=%d}",
                timeframe, timestamp, contractHash.substring(0, 8), tag, columns.size());
    }
}
