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

import java.util.List;

@ApplicationScoped
public class BaseFeature implements Feature{
    public static final String FEATURE_NAME = "base_candle_ohlcv";
    public static final String DESCRIPTION = "OHLCV данные свечи";
    public static final String DATA_TYPE = "numeric(18, 2)";
    private final Candle candle;
    private List<CandlestickDto> candlestickDtos;
    private BaseBarSeries series;
    private ClosePriceIndicator closePrice;

    @Inject
    public BaseFeature(Candle candle) {
        this.candle = candle;
    }

    @PostConstruct
    private void init(){
        // TODO: временно берем только 5 минутный таймфрейм
        Candle.CandleInstance instance = candle.getInstance(CandleTimeframe.CANDLE_5M);
        candlestickDtos = instance.getHistoricalBuffer().getList();

        series = new BaseBarSeriesBuilder()
                .withName(FEATURE_NAME)
                .withNumFactory(DecimalNumFactory.getInstance(2))
                .build();

        for (CandlestickDto candleDto : candlestickDtos) {
            Bar bar = CandlestickMapper.mapDtoToBar(candleDto);
            if (bar != null) {
                series.addBar(bar);
            }
        }

        closePrice = new ClosePriceIndicator(series);
    }

    @Override
    public AbstractIndicator<Num> getIndicator() {
        return closePrice;
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
