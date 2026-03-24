package artskif.trader.strategy.indicators.multi;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.ADXAngleIndicator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Multi-индикатор угла наклона кривой ADX.
 * <p>
 * Обёртка над {@link ADXAngleIndicator}, поддерживающая работу
 * с несколькими таймфреймами через {@link MultiAbstractIndicator}.
 * <p>
 * Период ADX и окно расчёта угла задаются константами
 * {@link #ADX_PERIOD} и {@link #ANGLE_BAR_COUNT}.
 */
@ApplicationScoped
public class ADXAngleIndicatorM extends MultiAbstractIndicator<ADXAngleIndicator> {

    /** Период ADX (количество баров для сглаживания). */
    public static final int ADX_PERIOD = 14;

    /** Количество баров для расчёта угла наклона. */
    public static final int ANGLE_BAR_COUNT = 1;

    public ADXAngleIndicatorM() {
        super(null);
    }

    @Inject
    public ADXAngleIndicatorM(Candle candle) {
        super(candle);
    }

    @Override
    protected ADXAngleIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new ADXAngleIndicator(getBarSeries(timeframe, isLifeSeries), ADX_PERIOD, ANGLE_BAR_COUNT);
    }
}
