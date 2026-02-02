package artskif.trader.strategy.contract.features.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.features.FeatureMetadata;
import artskif.trader.strategy.contract.features.FeatureTypeMetadata;
import artskif.trader.strategy.contract.features.AbstractFeature;
import artskif.trader.strategy.indicators.multi.RSIIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class RSIFeature extends AbstractFeature<RSIIndicatorM> {

    public static final int RSI_PERIOD = 14;

    /**
     * Перечислимый тип для различных значений RSI фичи
     */
    public enum RSIFeatureType implements FeatureTypeMetadata {
        RSI_5M(
            "feature_rsi_14_5m",
            "RSI индикатор с периодом 14 на таймфрейме 5m",
            "numeric(5, 2)",
            CandleTimeframe.CANDLE_5M
        ),
        RSI_4H(
            "feature_rsi_14_4h",
            "RSI индикатор с периодом 14 на таймфрейме 4h",
            "numeric(5, 2)",
            CandleTimeframe.CANDLE_4H
        ),
        RSI_5M_ON_4H(
            "feature_rsi_14_5m_on_4h",
            "RSI индикатор с периодом 14 на таймфрейме 4h для индекса 5m",
            "numeric(5, 2)",
            CandleTimeframe.CANDLE_5M,
            CandleTimeframe.CANDLE_4H  // higher timeframe
        );

        private final FeatureMetadata metadata;

        RSIFeatureType(String name, String description, String dataType, CandleTimeframe timeframe) {
            this.metadata = new FeatureMetadata(name, description, dataType, timeframe);
        }

        RSIFeatureType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe) {
            this.metadata = new FeatureMetadata(name, description, dataType, timeframe, higherTimeframe);
        }

        @Override
        public FeatureMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected RSIFeature() {
        super(null);
    }

    @Inject
    public RSIFeature(RSIIndicatorM rsiIndicatorM) {
        super(rsiIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        return getValueByNameGeneric(isLiveSeries, valueName, index, RSIFeatureType.values());
    }

    @Override
    public List<String> getFeatureValueNames() {
        return FeatureTypeMetadata.getNames(RSIFeatureType.values());
    }

    @Override
    public FeatureTypeMetadata getFeatureTypeMetadataByValueName(String name) {
        return FeatureTypeMetadata.findByName(RSIFeatureType.values(), name);
    }
}

