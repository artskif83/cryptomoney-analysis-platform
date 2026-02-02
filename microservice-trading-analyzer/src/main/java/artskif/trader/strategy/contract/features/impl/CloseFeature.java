package artskif.trader.strategy.contract.features.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.features.FeatureMetadata;
import artskif.trader.strategy.contract.features.FeatureTypeMetadata;
import artskif.trader.strategy.contract.features.AbstractFeature;
import artskif.trader.strategy.indicators.multi.CloseIndicatorM;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class CloseFeature extends AbstractFeature<CloseIndicatorM> {

    /**
     * Перечислимый тип для различных значений ADX фичи
     */
    public enum CloseFeatureType implements FeatureTypeMetadata {
        BASE_5M(
                "feature_close_5m",
                "Close индикатор на таймфрейме 5m",
                "numeric(18, 2)",
                CandleTimeframe.CANDLE_5M
        ),
        BASE_4H(
                "feature_close_4h",
                "Close индикатор на таймфрейме 4h",
                "numeric(18, 2)",
                CandleTimeframe.CANDLE_4H
        );

        private final FeatureMetadata metadata;

        CloseFeatureType(String name, String description, String dataType, CandleTimeframe timeframe) {
            this.metadata = new FeatureMetadata(name, description, dataType, timeframe);
        }

        @Override
        public FeatureMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected CloseFeature() {
        super(null);
    }

    @Inject
    public CloseFeature(CloseIndicatorM closeIndicatorM) {
        super(closeIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        return getValueByNameGeneric(isLiveSeries, valueName, index, CloseFeatureType.values());
    }

    @Override
    public List<String> getFeatureValueNames() {
        return FeatureTypeMetadata.getNames(CloseFeatureType.values());
    }

    @Override
    public FeatureTypeMetadata getFeatureTypeMetadataByValueName(String name) {
        return FeatureTypeMetadata.findByName(CloseFeatureType.values(), name);
    }
}
