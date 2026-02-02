package artskif.trader.strategy.contract.features.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.features.FeatureMetadata;
import artskif.trader.strategy.contract.features.FeatureTypeMetadata;
import artskif.trader.strategy.contract.features.AbstractFeature;
import artskif.trader.strategy.indicators.multi.ADXIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class ADXFeature extends AbstractFeature<ADXIndicatorM> {

    public static final int ADX_PERIOD = 14;

    /**
     * Перечислимый тип для различных значений ADX фичи
     */
    public enum ADXFeatureType implements FeatureTypeMetadata {
        ADX_5M(
                "feature_adx_14_5m",
                "ADX индикатор с периодом 14 на таймфрейме 5m",
                "numeric(5, 2)",
                CandleTimeframe.CANDLE_5M
        ),
        ADX_4H(
                "feature_adx_14_4h",
                "ADX индикатор с периодом 14 на таймфрейме 4h",
                "numeric(5, 2)",
                CandleTimeframe.CANDLE_4H
        ),
        ADX_5M_ON_4H(
                "feature_adx_14_5m_on_4h",
                "ADX индикатор с периодом 14 на таймфрейме 4h для индекса 5m",
                "numeric(5, 2)",
                CandleTimeframe.CANDLE_5M,
                CandleTimeframe.CANDLE_4H  // higher timeframe
        );

        private final FeatureMetadata metadata;

        ADXFeatureType(String name, String description, String dataType, CandleTimeframe timeframe) {
            this.metadata = new FeatureMetadata(name, description, dataType, timeframe);
        }

        ADXFeatureType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe) {
            this.metadata = new FeatureMetadata(name, description, dataType, timeframe, higherTimeframe);
        }

        @Override
        public FeatureMetadata getMetadata() {
            return metadata;
        }
    }


    // No-args constructor required by CDI
    protected ADXFeature() {
        super(null);
    }

    @Inject
    public ADXFeature(ADXIndicatorM adxIndicatorM) {
        super(adxIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        return getValueByNameGeneric(isLiveSeries, valueName, index, ADXFeatureType.values());
    }

    @Override
    public List<String> getFeatureValueNames() {
        return FeatureTypeMetadata.getNames(ADXFeatureType.values());
    }

    @Override
    public FeatureTypeMetadata getFeatureTypeMetadataByValueName(String name) {
        return FeatureTypeMetadata.findByName(ADXFeatureType.values(), name);
    }
}
