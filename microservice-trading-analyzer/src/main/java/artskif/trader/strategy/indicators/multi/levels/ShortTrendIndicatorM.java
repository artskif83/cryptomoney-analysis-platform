package artskif.trader.strategy.indicators.multi.levels;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.ShortTrendIndicator;
import artskif.trader.strategy.indicators.multi.ADXAngleIndicatorM;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.DoubleMAIndicatorM;
import artskif.trader.strategy.indicators.multi.HighPriceIndicatorM;
import artskif.trader.strategy.indicators.multi.levels.ShortHighLevelIndicatorM;
import artskif.trader.strategy.indicators.multi.levels.LongHighLevelIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;

@ApplicationScoped
public class ShortTrendIndicatorM extends MultiAbstractIndicator<ShortTrendIndicator> {

    private final HighPriceIndicatorM highPriceIndicatorM;
    private final ClosePriceIndicatorM closePriceIndicatorM;
    private final DoubleMAIndicatorM doubleMAIndicatorM;
    private final ADXAngleIndicatorM adxAngleIndicatorM;
    private final LongHighLevelIndicatorM longHighLevelIndicatorM;
    private final ShortHighLevelIndicatorM shortHighLevelIndicatorM;


    // No-args constructor required by CDI
    protected ShortTrendIndicatorM() {
        super(null);
        this.highPriceIndicatorM = null;
        this.closePriceIndicatorM = null;
        this.doubleMAIndicatorM = null;
        this.adxAngleIndicatorM = null;
        this.longHighLevelIndicatorM = null;
        this.shortHighLevelIndicatorM = null;
    }

    @Inject
    public ShortTrendIndicatorM(Candle candle,
                                HighPriceIndicatorM highPriceIndicatorM,
                                ClosePriceIndicatorM closePriceIndicatorM,
                                DoubleMAIndicatorM doubleMAIndicatorM,
                                ADXAngleIndicatorM adxAngleIndicatorM,
                                LongHighLevelIndicatorM longHighLevelIndicatorM,
                                ShortHighLevelIndicatorM shortHighLevelIndicatorM
    ) {
        super(candle);
        this.highPriceIndicatorM = highPriceIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
        this.doubleMAIndicatorM = doubleMAIndicatorM;
        this.adxAngleIndicatorM = adxAngleIndicatorM;
        this.longHighLevelIndicatorM = longHighLevelIndicatorM;
        this.shortHighLevelIndicatorM = shortHighLevelIndicatorM;
    }

    @Override
    protected ShortTrendIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new ShortTrendIndicator(
                highPriceIndicatorM.getIndicator(CandleTimeframe.CANDLE_1M, isLifeSeries),
                doubleMAIndicatorM.getIndicator(CandleTimeframe.CANDLE_5M, isLifeSeries),
                doubleMAIndicatorM.getIndicator(CandleTimeframe.CANDLE_1H, isLifeSeries),
                doubleMAIndicatorM.getIndicator(CandleTimeframe.CANDLE_1W, isLifeSeries),
                adxAngleIndicatorM.getIndicator(CandleTimeframe.CANDLE_1H, isLifeSeries),
                longHighLevelIndicatorM.getIndicator(CandleTimeframe.CANDLE_1H, isLifeSeries),
                shortHighLevelIndicatorM.getIndicator(CandleTimeframe.CANDLE_1H, isLifeSeries),
                closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                5,
                DecimalNum.valueOf(0.05),
                DecimalNum.valueOf(0.20),
                DecimalNum.valueOf(0.2));
    }
}
