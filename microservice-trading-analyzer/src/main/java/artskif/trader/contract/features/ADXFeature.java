package artskif.trader.contract.features;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.entity.MetadataType;
import jakarta.enterprise.context.ApplicationScoped;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.num.Num;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ADXFeature implements Feature {

    public static final String FEATURE_NAME = "feature_adx_14";
    public static final String DESCRIPTION = "ADX индикатор с периодом 14";
    public static final String DATA_TYPE = "numeric(5, 2)";
    public static final int ADX_PERIOD = 14;
    private final Map<CandleTimeframe, ADXIndicator> adxIndicator = Map.of();
    private final BaseFeature baseFeature;

    public ADXFeature(BaseFeature baseFeature) {
        this.baseFeature = baseFeature;
        this.adxIndicator.put(CandleTimeframe.CANDLE_5M, new ADXIndicator(baseFeature.getIndicator(CandleTimeframe.CANDLE_5M).getBarSeries(), ADX_PERIOD));
        this.adxIndicator.put(CandleTimeframe.CANDLE_4H, new ADXIndicator(baseFeature.getIndicator(CandleTimeframe.CANDLE_4H).getBarSeries(), ADX_PERIOD));
    }

    public static ContractMetadata getFeatureMetadata(Integer sequenceOrder, Contract contract) {
        return new ContractMetadata(FEATURE_NAME, DESCRIPTION, sequenceOrder, DATA_TYPE, MetadataType.FEATURE, contract);
    }


    @Override
    public List<CandlestickDto> getCandlestickDtos() {
        return baseFeature.getCandlestickDtos();
    }

    @Override
    public AbstractIndicator<Num> getIndicator(CandleTimeframe timeframe) {
        return adxIndicator.get(timeframe);
    }

    @Override
    public String getFeatureName() {
        return FEATURE_NAME;
    }

    @Override
    public String getDataType() {
        return DATA_TYPE;
    }
}
