package artskif.trader.contract.features;

import artskif.trader.candle.CandleTimeframe;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class ADXFeature extends AbstractFeature<ADXIndicator> {

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
                CandleTimeframe.CANDLE_5M
        );

        private final FeatureMetadata metadata;

        ADXFeatureType(String name, String description, String dataType, CandleTimeframe timeframe) {
            this.metadata = new FeatureMetadata(name, description, dataType, timeframe);
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
    public ADXFeature(BaseFeature baseFeature) {
        super(baseFeature);
        this.indicators.put(CandleTimeframe.CANDLE_5M, new ADXIndicator(baseFeature.getIndicator(CandleTimeframe.CANDLE_5M).getBarSeries(), ADX_PERIOD));
        this.indicators.put(CandleTimeframe.CANDLE_4H, new ADXIndicator(baseFeature.getIndicator(CandleTimeframe.CANDLE_4H).getBarSeries(), ADX_PERIOD));
    }

    @Override
    public Num getValueByName(String valueName, int index) {
        ADXFeatureType adxType = FeatureTypeMetadata.findByName(ADXFeatureType.values(), valueName);

        return switch (adxType) {
            case ADX_5M -> indicators.get(CandleTimeframe.CANDLE_5M).getValue(index);
            case ADX_4H -> indicators.get(CandleTimeframe.CANDLE_4H).getValue(index);
            case ADX_5M_ON_4H -> getHigherTimeframeValue(index, CandleTimeframe.CANDLE_5M, CandleTimeframe.CANDLE_4H);
        };
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
