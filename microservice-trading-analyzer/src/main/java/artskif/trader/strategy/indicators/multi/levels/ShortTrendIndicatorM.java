package artskif.trader.strategy.indicators.multi.levels;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.ShortTrendIndicator;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.HighPriceIndicatorM;
import artskif.trader.strategy.indicators.multi.RSIIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;

@ApplicationScoped
public class ShortTrendIndicatorM extends MultiAbstractIndicator<ShortTrendIndicator> {

    private final HighPriceIndicatorM highPriceIndicatorM;
    private final ClosePriceIndicatorM closePriceIndicatorM;
    private final RSIIndicatorM rsiIndicatorM;

    // No-args constructor required by CDI
    protected ShortTrendIndicatorM() {
        super(null);
        this.highPriceIndicatorM = null;
        this.closePriceIndicatorM = null;
        this.rsiIndicatorM = null;
    }

    @Inject
    public ShortTrendIndicatorM(Candle candle,
                                HighPriceIndicatorM highPriceIndicatorM,
                                ClosePriceIndicatorM closePriceIndicatorM,
                                RSIIndicatorM rsiIndicatorM
    ) {
        super(candle);
        this.highPriceIndicatorM = highPriceIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
        this.rsiIndicatorM = rsiIndicatorM;
    }

    @Override
    protected ShortTrendIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new ShortTrendIndicator(
                highPriceIndicatorM.getIndicator(CandleTimeframe.CANDLE_1M, isLifeSeries),
                closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                rsiIndicatorM.getIndicator(CandleTimeframe.CANDLE_5M, isLifeSeries),
                5,
                DecimalNum.valueOf(0.1),
                DecimalNum.valueOf(0.5),
                DecimalNum.valueOf(0.2));
    }
}
