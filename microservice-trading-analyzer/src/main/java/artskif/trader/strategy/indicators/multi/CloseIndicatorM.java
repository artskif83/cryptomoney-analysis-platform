package artskif.trader.strategy.indicators.multi;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

@ApplicationScoped
public class CloseIndicatorM extends MultiAbstractIndicator<ClosePriceIndicator> {

    // No-args constructor required by CDI
    protected CloseIndicatorM() {
        super(null);
    }

    @Inject
    public CloseIndicatorM(Candle candle) {
        super(candle);
    }

    @Override
    protected ClosePriceIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new ClosePriceIndicator(getBarSeries(timeframe, isLifeSeries));
    }
}
