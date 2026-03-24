package artskif.trader.strategy.indicators.multi.levels;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.LongTrendIndicator;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.DoubleMAIndicatorM;
import artskif.trader.strategy.indicators.multi.LowPriceIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;

@ApplicationScoped
public class LongTrendIndicatorM extends MultiAbstractIndicator<LongTrendIndicator> {

    private final LowPriceIndicatorM lowPriceIndicatorM;
    private final ClosePriceIndicatorM closePriceIndicatorM;
    private final DoubleMAIndicatorM doubleMAIndicatorM;

    // No-args constructor required by CDI
    protected LongTrendIndicatorM() {
        super(null);
        this.lowPriceIndicatorM = null;
        this.closePriceIndicatorM = null;
        this.doubleMAIndicatorM = null;
    }

    @Inject
    public LongTrendIndicatorM(Candle candle,
                               LowPriceIndicatorM lowPriceIndicatorM,
                               ClosePriceIndicatorM closePriceIndicatorM,
                               DoubleMAIndicatorM doubleMAIndicatorM) {
        super(candle);
        this.lowPriceIndicatorM = lowPriceIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
        this.doubleMAIndicatorM = doubleMAIndicatorM;
    }

    @Override
    protected LongTrendIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new LongTrendIndicator(
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
