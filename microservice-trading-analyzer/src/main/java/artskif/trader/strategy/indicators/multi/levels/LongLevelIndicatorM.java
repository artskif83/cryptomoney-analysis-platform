package artskif.trader.strategy.indicators.multi.levels;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.LongLevelIndicator;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.DoubleMAIndicatorM;
import artskif.trader.strategy.indicators.multi.LowPriceIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;

@ApplicationScoped
public class LongLevelIndicatorM extends MultiAbstractIndicator<LongLevelIndicator> {

    private final LowPriceIndicatorM lowPriceIndicatorM;
    private final ClosePriceIndicatorM closePriceIndicatorM;
    private final DoubleMAIndicatorM doubleMAIndicatorM;

    // No-args constructor required by CDI
    protected LongLevelIndicatorM() {
        super(null);
        this.lowPriceIndicatorM = null;
        this.closePriceIndicatorM = null;
        this.doubleMAIndicatorM = null;
    }

    @Inject
    public LongLevelIndicatorM(Candle candle,
                               LowPriceIndicatorM lowPriceIndicatorM,
                               ClosePriceIndicatorM closePriceIndicatorM,
                               DoubleMAIndicatorM doubleMAIndicatorM) {
        super(candle);
        this.lowPriceIndicatorM = lowPriceIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
        this.doubleMAIndicatorM = doubleMAIndicatorM;
    }

    @Override
    protected LongLevelIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new LongLevelIndicator(
                lowPriceIndicatorM.getIndicator(CandleTimeframe.CANDLE_1M, isLifeSeries),
                doubleMAIndicatorM.getIndicator(CandleTimeframe.CANDLE_5M, isLifeSeries),
                doubleMAIndicatorM.getIndicator(CandleTimeframe.CANDLE_1H, isLifeSeries),
                closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                5,
                DecimalNum.valueOf(0.05),
                DecimalNum.valueOf(0.1),
                DecimalNum.valueOf(0.15));
    }
}
