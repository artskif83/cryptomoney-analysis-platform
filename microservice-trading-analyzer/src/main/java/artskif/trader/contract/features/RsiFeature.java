package artskif.trader.contract.features;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.entity.MetadataType;
import jakarta.enterprise.context.ApplicationScoped;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class RsiFeature implements Feature {

    public static final String FEATURE_NAME = "feature_rsi_14";
    public static final String DESCRIPTION = "RSI индикатор с периодом 14";
    public static final String DATA_TYPE = "numeric(5, 2)";
    public static final int RSI_PERIOD = 14;
    private final RSIIndicator rsiIndicator;
    private final BaseFeature baseFeature;

    public RsiFeature(BaseFeature baseFeature) {
        this.baseFeature = baseFeature;
        this.rsiIndicator = new RSIIndicator(baseFeature.getIndicator(), RSI_PERIOD);
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
        return rsiIndicator;
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

