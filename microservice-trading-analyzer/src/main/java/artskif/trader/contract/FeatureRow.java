package artskif.trader.contract;

import artskif.trader.candle.CandleTimeframe;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Строка с фичами для одной свечи
 * Содержит все вычисленные значения фич и хеш контракта
 */
public class FeatureRow {

    @Getter
    private final Duration timeframe;
    @Getter
    private final Instant timestamp;
    @Getter
    private final String contractHash;
    private final Map<String, Object> features;

    public FeatureRow(Duration timeframe, Instant timestamp,
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
    public String toString() {
        return String.format("FeatureRow{tf=%s, ts=%s, hash='%s', features=%d}",
                timeframe, timestamp, contractHash.substring(0, 8), features.size());
    }
}

