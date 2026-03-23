package artskif.trader.strategy.indicators.multi.levels;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.ShortLevelIndicator;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.DoubleMAIndicatorM;
import artskif.trader.strategy.indicators.multi.HighPriceIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;

@ApplicationScoped
public class ShortLevelIndicatorM extends MultiAbstractIndicator<ShortLevelIndicator> {

    private final HighPriceIndicatorM highPriceIndicatorM;
    private final ClosePriceIndicatorM closePriceIndicatorM;
    private final DoubleMAIndicatorM doubleMAIndicatorM;


    // No-args constructor required by CDI
    protected ShortLevelIndicatorM() {
        super(null);
        this.highPriceIndicatorM = null;
        this.closePriceIndicatorM = null;
        this.doubleMAIndicatorM = null;
    }

    @Inject
    public ShortLevelIndicatorM(Candle candle,
                                HighPriceIndicatorM highPriceIndicatorM,
                                ClosePriceIndicatorM closePriceIndicatorM,
                                DoubleMAIndicatorM doubleMAIndicatorM) {
        super(candle);
        this.highPriceIndicatorM = highPriceIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
        this.doubleMAIndicatorM = doubleMAIndicatorM;
    }

    @Override
    protected ShortLevelIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new ShortLevelIndicator(
                highPriceIndicatorM.getIndicator(CandleTimeframe.CANDLE_1M, isLifeSeries),
                doubleMAIndicatorM.getIndicator(CandleTimeframe.CANDLE_5M, isLifeSeries),
                doubleMAIndicatorM.getIndicator(CandleTimeframe.CANDLE_1H, isLifeSeries),
                closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                10,
                DecimalNum.valueOf(0.05),
                DecimalNum.valueOf(0.1),
                DecimalNum.valueOf(0.15));
    }
}
