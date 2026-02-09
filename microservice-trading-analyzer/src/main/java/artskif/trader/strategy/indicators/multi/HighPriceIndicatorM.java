package artskif.trader.strategy.indicators.multi;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;

@ApplicationScoped
public class HighPriceIndicatorM extends MultiAbstractIndicator<HighPriceIndicator> {

    // No-args constructor required by CDI
    protected HighPriceIndicatorM() {
        super(null);
    }

    @Inject
    public HighPriceIndicatorM(Candle candle) {
        super(candle);
    }

    @Override
    protected HighPriceIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new HighPriceIndicator(getBarSeries(timeframe, isLifeSeries));
    }
}
