package artskif.trader.contract.features.impl;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.contract.features.FeatureMetadata;
import artskif.trader.contract.features.FeatureTypeMetadata;
import artskif.trader.contract.features.AbstractFeature;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.mapper.CandlestickMapper;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNumFactory;
import org.ta4j.core.num.Num;
import org.ta4j.core.utils.BarSeriesUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BaseFeature extends AbstractFeature<ClosePriceIndicator> {
    private final Candle candle;
    private final Map<CandleTimeframe, List<CandlestickDto>> candlestickDtos = new HashMap<>();

    /**
     * Перечислимый тип для различных значений ADX фичи
     */
    public enum BaseFeatureType implements FeatureTypeMetadata {
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

        BaseFeatureType(String name, String description, String dataType, CandleTimeframe timeframe) {
            this.metadata = new FeatureMetadata(name, description, dataType, timeframe);
        }

        @Override
        public FeatureMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected BaseFeature() {
        super(null);
        this.candle = null;
    }

    @Inject
    public BaseFeature(Candle candle) {
        super(null);
        this.candle = candle;
    }

    @PostConstruct
    private void init(){
        Candle.CandleInstance instance = candle.getInstance(CandleTimeframe.CANDLE_5M);
        List<CandlestickDto> candlestickDtoList = instance.getHistoricalBuffer().getList();
        candlestickDtos.put(CandleTimeframe.CANDLE_5M, candlestickDtoList);
        BaseBarSeries baseBarSeries = new BaseBarSeriesBuilder()
                .withName(BaseFeatureType.BASE_5M.getName())
                .withNumFactory(DecimalNumFactory.getInstance(2))
                .build();

        for (CandlestickDto candleDto : candlestickDtoList) {
            Bar bar = CandlestickMapper.mapDtoToBar(candleDto);
            if (bar != null) {
                baseBarSeries.addBar(bar);
            }
        }

        // Находим индекс первого бара, который начинается ровно в 00:00 (начало суток)
        int firstAlignedBarIndex = -1;
        for (int i = 0; i < baseBarSeries.getBarCount(); i++) {
            Bar bar = baseBarSeries.getBar(i);
            java.time.Instant beginTime = bar.getBeginTime();

            // Проверяем, что время выровнено по суткам (00:00 часов)
            long epochSeconds = beginTime.getEpochSecond();
            long secondsInDay = 86400; // 24 часа * 3600 секунд
            if (epochSeconds % secondsInDay == 0) {
                firstAlignedBarIndex = i;
                break;
            }
        }

        // Если найден выровненный бар, создаём новую серию без начальных баров
        BaseBarSeries alignedBarSeries = baseBarSeries;
        if (firstAlignedBarIndex > 0) {
            Log.infof("🔧 Удаляем %d начальных баров для выравнивания с 00:00", firstAlignedBarIndex);
            alignedBarSeries = new BaseBarSeriesBuilder()
                    .withName(BaseFeatureType.BASE_5M.getName())
                    .withNumFactory(DecimalNumFactory.getInstance(2))
                    .build();

            for (int i = firstAlignedBarIndex; i < baseBarSeries.getBarCount(); i++) {
                alignedBarSeries.addBar(baseBarSeries.getBar(i));
            }
        }

        this.indicators.put(CandleTimeframe.CANDLE_5M, new ClosePriceIndicator(alignedBarSeries));
        this.indicators.put(CandleTimeframe.CANDLE_4H, new ClosePriceIndicator(
                BarSeriesUtils.aggregateBars(alignedBarSeries, CandleTimeframe.CANDLE_4H.getDuration(),"baseBarSeries4H")));
        Log.infof("✅ BaseFeature проинициализирована для таймфреймов: %s", indicators.size());
    }


    @Override
    public Num getValueByName(String valueName, int index) {
        BaseFeatureType adxType = FeatureTypeMetadata.findByName(BaseFeatureType.values(), valueName);

        return switch (adxType) {
            case BASE_5M -> indicators.get(CandleTimeframe.CANDLE_5M).getValue(index);
            case BASE_4H -> indicators.get(CandleTimeframe.CANDLE_4H).getValue(index);
        };
    }

    @Override
    public List<CandlestickDto> getCandlestickDtos(CandleTimeframe timeframe) {
        return candlestickDtos.get(timeframe);
    }

    @Override
    public List<String> getFeatureValueNames() {
        return FeatureTypeMetadata.getNames(BaseFeatureType.values());
    }

    @Override
    public FeatureTypeMetadata getFeatureTypeMetadataByValueName(String name) {
        return FeatureTypeMetadata.findByName(BaseFeatureType.values(), name);
    }
}
