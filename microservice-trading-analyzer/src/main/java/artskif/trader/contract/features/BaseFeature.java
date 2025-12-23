package artskif.trader.contract.features;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.mapper.CandlestickMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNumFactory;
import org.ta4j.core.num.Num;
import org.ta4j.core.utils.BarSeriesUtils;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BaseFeature implements Feature{
    public static final String FEATURE_NAME = "base_candle_ohlcv";
    public static final String DESCRIPTION = "OHLCV данные свечи";
    public static final String DATA_TYPE = "numeric(18, 2)";
    private final Candle candle;
    private List<CandlestickDto> candlestickDtos;
    private Map<CandleTimeframe, BaseBarSeries> series;
    private Map<CandleTimeframe, ClosePriceIndicator> closePrice;

    @Inject
    public BaseFeature(Candle candle) {
        this.candle = candle;
    }

    @PostConstruct
    private void init(){
        Candle.CandleInstance instance = candle.getInstance(CandleTimeframe.CANDLE_5M);
        candlestickDtos = instance.getHistoricalBuffer().getList();
        BaseBarSeries baseBarSeries = new BaseBarSeriesBuilder()
                .withName(FEATURE_NAME)
                .withNumFactory(DecimalNumFactory.getInstance(2))
                .build();

        for (CandlestickDto candleDto : candlestickDtos) {
            Bar bar = CandlestickMapper.mapDtoToBar(candleDto);
            if (bar != null) {
                baseBarSeries.addBar(bar);
            }
        }
        series.put(CandleTimeframe.CANDLE_5M, baseBarSeries);
        series.put(CandleTimeframe.CANDLE_4H,
                (BaseBarSeries) BarSeriesUtils.aggregateBars(baseBarSeries, CandleTimeframe.CANDLE_4H.getDuration(),"baseBarSeries4H"));

        closePrice.put(CandleTimeframe.CANDLE_5M, new ClosePriceIndicator(series.get(CandleTimeframe.CANDLE_5M)));
        closePrice.put(CandleTimeframe.CANDLE_4H, new ClosePriceIndicator(series.get(CandleTimeframe.CANDLE_4H)));
    }

    @Override
    public AbstractIndicator<Num> getIndicator(CandleTimeframe timeframe) {
        return closePrice.get(timeframe);
    }

    @Override
    public List<CandlestickDto> getCandlestickDtos() {
        return candlestickDtos;
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
