package artskif.trader.strategy.contract.snapshot.impl;

import artskif.trader.strategy.contract.snapshot.ContractSnapshot;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class ContractSnapshotRow implements ContractSnapshot {

    private final Duration timeframe;
    private final Instant timestamp;
    private final String contractHash;
    private final Map<String, Object> features;

    public ContractSnapshotRow(Duration timeframe, Instant timestamp,
                               String contractHash) {
        this.timeframe = timeframe;
        this.timestamp = timestamp;
        this.contractHash = contractHash;
        this.features = new HashMap<>();
    }

    /**
     * Добавить фичу в строку
     */
    public void addFeature(String featureName, Object value) {
        features.put(featureName, value);
    }

    /**
     * Получить значение фичи
     */
    public Object getFeature(String featureName) {
        return features.get(featureName);
    }

    /**
     * Получить все фичи
     */
    public Map<String, Object> getAllFeatures() {
        return new HashMap<>(features);
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
    public String toString() {
        return String.format("FeatureRow{tf=%s, ts=%s, hash='%s', features=%d}",
                timeframe, timestamp, contractHash.substring(0, 8), features.size());
    }
}
