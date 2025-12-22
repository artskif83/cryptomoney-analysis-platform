package artskif.trader.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Wide Table для обучения ML модели на базе XGBoost
 * Содержит базовые поля из candles и динамически добавляемые фичи из индикаторов
 */
@Entity
@Table(name = "features")
public class Feature extends PanacheEntityBase {

    @EmbeddedId
    public CandleId id;

    @Column(nullable = false, precision = 18, scale = 8)
    public BigDecimal open;

    @Column(nullable = false, precision = 18, scale = 8)
    public BigDecimal high;

    @Column(nullable = false, precision = 18, scale = 8)
    public BigDecimal low;

    @Column(nullable = false, precision = 18, scale = 8)
    public BigDecimal close;

    @Column(nullable = true, precision = 30, scale = 8)
    public BigDecimal volume;

    @Column(nullable = false, columnDefinition = "boolean default true")
    public boolean confirmed;

    /**
     * Динамические атрибуты - фичи для ML
     * Ключ - имя фичи (feature_name), значение - значение фичи
     * Не сохраняется в БД напрямую, используется для работы с фичами в коде
     */
    @Transient
    private Map<String, Object> features = new HashMap<>();

    public Feature() {
    }

    public Feature(CandleId id, BigDecimal open, BigDecimal high,
                    BigDecimal low, BigDecimal close, BigDecimal volume, boolean confirmed) {
        this.id = id;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.confirmed = confirmed;
    }

    public void addFeature(String featureName, Object value) {
        features.put(featureName, value);
    }

    public Object getFeature(String featureName) {
        return features.get(featureName);
    }

    public Map<String, Object> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, Object> features) {
        this.features = features;
    }
}

