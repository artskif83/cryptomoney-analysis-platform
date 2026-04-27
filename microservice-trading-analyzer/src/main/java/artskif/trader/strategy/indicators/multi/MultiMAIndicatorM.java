package artskif.trader.strategy.indicators.multi;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.MultiMAIndicator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Мульти-таймфреймовый индикатор двойной скользящей средней.
 * Позволяет использовать DoubleMAIndicator на разных таймфреймах.
 */
@ApplicationScoped
public class MultiMAIndicatorM extends MultiAbstractIndicator<MultiMAIndicator> {

    private final ClosePriceIndicatorM closeIndicator;

    // No-args constructor required by CDI
    protected MultiMAIndicatorM() {
        super(null);
        this.closeIndicator = null;
    }

    @Inject
    public MultiMAIndicatorM(Candle candle, ClosePriceIndicatorM closeIndicator) {
        super(candle);
        this.closeIndicator = closeIndicator;
    }

    @Override
    protected MultiMAIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new MultiMAIndicator(
                closeIndicator.getIndicator(timeframe, isLifeSeries)
        );
    }
}
