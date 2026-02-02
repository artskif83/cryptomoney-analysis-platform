package artskif.trader.strategy.indicators.multi;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import jakarta.enterprise.context.ApplicationScoped;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;

@ApplicationScoped
public class ADXIndicatorM extends MultiAbstractIndicator<ADXIndicator> {

    public static final int ADX_PERIOD = 14;

    public ADXIndicatorM() {
        super(null);
    }

    public ADXIndicatorM(Candle candle) {
        super(candle);
    }

    @Override
    protected ADXIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new ADXIndicator(getBarSeries(timeframe, isLifeSeries), ADX_PERIOD);
    }


}
