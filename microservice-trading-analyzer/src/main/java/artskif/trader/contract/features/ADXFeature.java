package artskif.trader.contract.features;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.entity.MetadataType;
import jakarta.enterprise.context.ApplicationScoped;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class ADXFeature implements Feature {

    public static final String FEATURE_NAME = "feature_adx_14";
    public static final String DESCRIPTION = "ADX индикатор с периодом 14";
    public static final String DATA_TYPE = "numeric(5, 2)";
    public static final int ADX_PERIOD = 14;
    private final ADXIndicator adxIndicator;
    private final BaseFeature baseFeature;

    public ADXFeature(BaseFeature baseFeature) {
        this.baseFeature = baseFeature;
        this.adxIndicator = new ADXIndicator(baseFeature.getIndicator().getBarSeries(), ADX_PERIOD);
    }

    public static ContractMetadata getFeatureMetadata(Integer sequenceOrder, Contract contract) {
        return new ContractMetadata(FEATURE_NAME, DESCRIPTION, sequenceOrder, DATA_TYPE, MetadataType.FEATURE, contract);
    }

    @Override
    public List<CandlestickDto> getCandlestickDtos() {
        return baseFeature.getCandlestickDtos();
    }

    @Override
    public AbstractIndicator<Num> getIndicator() {
        return adxIndicator;
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
