package artskif.trader.contract.labels;


import artskif.trader.candle.CandleTimeframe;
import artskif.trader.contract.features.BaseFeature;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.entity.MetadataType;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@ApplicationScoped
public class FutureReturnLabel implements Label {

    public static final String LABEL_NAME = "label_future_up_1p";
    public static final String DESCRIPTION = "Цена через 10 баров выше на 1%";
    public static final String DATA_TYPE = "boolean";

    private static final int HORIZON = 10;
    private static final BigDecimal THRESHOLD = new BigDecimal("0.01");

    private final BaseFeature baseFeature;

    public FutureReturnLabel(BaseFeature baseFeature) {
        this.baseFeature = baseFeature;
    }

    public static ContractMetadata getLabelMetadata(Integer sequenceOrder, Contract contract) {
        return new ContractMetadata(LABEL_NAME, DESCRIPTION, sequenceOrder, DATA_TYPE, MetadataType.LABEL, contract);
    }

    @Override
    public String getLabelName() {
        return LABEL_NAME;
    }

    @Override
    public String getDataType() {
        return "numeric(3, 0)";
    }

    @Override
    public List<CandlestickDto> getCandlestickDtos(CandleTimeframe timeframe) {
        return baseFeature.getCandlestickDtos(timeframe);
    }

    @Override
    public BigDecimal getValue(CandleTimeframe timeframe, int index) {
        List<CandlestickDto> candles = getCandlestickDtos(timeframe);
        if (index + HORIZON >= candles.size()) {
            // Хвост можно пометить как null или 0
            return BigDecimal.ZERO;
        }
        BigDecimal current = candles.get(index).getClose();
        BigDecimal future = candles.get(index + HORIZON).getClose();
        BigDecimal ret = future.subtract(current)
                .divide(current, 8, RoundingMode.HALF_UP);
        return ret.compareTo(THRESHOLD) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
    }
}

