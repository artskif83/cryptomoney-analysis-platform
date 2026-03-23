package artskif.trader.strategy.indicators.multi.levels;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.ShortLowLevelIndicator;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.LowPriceIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;

@ApplicationScoped
public class ShortLowLevelIndicatorM extends MultiAbstractIndicator<ShortLowLevelIndicator> {

    private final LowPriceIndicatorM lowPriceIndicatorM;
    private final ClosePriceIndicatorM closePriceIndicatorM;

    // No-args constructor required by CDI
    protected ShortLowLevelIndicatorM() {
        super(null);
        this.lowPriceIndicatorM = null;
        this.closePriceIndicatorM = null;
    }

    @Inject
    public ShortLowLevelIndicatorM(Candle candle,
                                   LowPriceIndicatorM lowPriceIndicatorM,
                                   ClosePriceIndicatorM closePriceIndicatorM) {
        super(candle);
        this.lowPriceIndicatorM = lowPriceIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
    }

    @Override
    protected ShortLowLevelIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new ShortLowLevelIndicator(
                lowPriceIndicatorM.getIndicator(CandleTimeframe.CANDLE_1M, isLifeSeries),
                closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                6,
                6,
                DecimalNum.valueOf(0.05),
                DecimalNum.valueOf(0.1),
                DecimalNum.valueOf(0.1),
                DecimalNum.valueOf(0.05));
    }
}
