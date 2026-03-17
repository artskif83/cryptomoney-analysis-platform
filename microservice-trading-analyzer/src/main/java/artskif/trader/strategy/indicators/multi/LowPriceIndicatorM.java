package artskif.trader.strategy.indicators.multi;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;

@ApplicationScoped
public class LowPriceIndicatorM extends MultiAbstractIndicator<LowPriceIndicator> {

    // No-args constructor required by CDI
    protected LowPriceIndicatorM() {
        super(null);
    }

    @Inject
    public LowPriceIndicatorM(Candle candle) {
        super(candle);
    }

    @Override
    protected LowPriceIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new LowPriceIndicator(getBarSeries(timeframe, isLifeSeries));
    }
}
