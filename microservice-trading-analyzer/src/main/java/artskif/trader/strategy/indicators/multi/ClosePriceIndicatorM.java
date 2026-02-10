package artskif.trader.strategy.indicators.multi;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

@ApplicationScoped
public class ClosePriceIndicatorM extends MultiAbstractIndicator<ClosePriceIndicator> {

    // No-args constructor required by CDI
    protected ClosePriceIndicatorM() {
        super(null);
    }

    @Inject
    public ClosePriceIndicatorM(Candle candle) {
        super(candle);
    }

    @Override
    protected ClosePriceIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new ClosePriceIndicator(getBarSeries(timeframe, isLifeSeries));
    }
}
