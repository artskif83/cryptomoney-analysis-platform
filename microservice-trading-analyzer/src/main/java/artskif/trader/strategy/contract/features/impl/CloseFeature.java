package artskif.trader.strategy.contract.features.impl;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleInstance;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.features.FeatureMetadata;
import artskif.trader.strategy.contract.features.FeatureTypeMetadata;
import artskif.trader.strategy.contract.features.AbstractFeature;
import artskif.trader.dto.CandlestickDto;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CloseFeature extends AbstractFeature<ClosePriceIndicator> {
    private final Candle candle;

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
        this.candle = null;
    }

    @Inject
    public CloseFeature(Candle candle) {
        super(null);
        this.candle = candle;
    }

    @PostConstruct
    private void init(){
        CandleInstance instance5m = candle.getInstance(CandleTimeframe.CANDLE_5M);
        CandleInstance instance4h = candle.getInstance(CandleTimeframe.CANDLE_4H);


        this.indicators.put(CandleTimeframe.CANDLE_5M, new ClosePriceIndicator(instance5m.getLiveBarSeries()));
        this.indicators.put(CandleTimeframe.CANDLE_4H, new ClosePriceIndicator(instance4h.getLiveBarSeries()));
    }


    @Override
    public Num getValueByName(String valueName, int index) {
        CloseFeatureType closeFeatureType = FeatureTypeMetadata.findByName(CloseFeatureType.values(), valueName);

        return switch (closeFeatureType) {
            case BASE_5M -> indicators.get(CandleTimeframe.CANDLE_5M).getValue(index);
            case BASE_4H -> indicators.get(CandleTimeframe.CANDLE_4H).getValue(index);
        };
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
