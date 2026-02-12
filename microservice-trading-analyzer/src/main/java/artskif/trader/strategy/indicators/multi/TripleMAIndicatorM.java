package artskif.trader.strategy.indicators.multi;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.TripleMAIndicator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Мульти-таймфреймовый индикатор тройной скользящей средней.
 * Позволяет использовать TripleMAIndicator на разных таймфреймах.
 */
@ApplicationScoped
public class TripleMAIndicatorM extends MultiAbstractIndicator<TripleMAIndicator> {

    public static final int DEFAULT_FAST_PERIOD = 3;
    public static final int DEFAULT_MEDIUM_PERIOD = 9;
    public static final int DEFAULT_SLOW_PERIOD = 27;
    public static final int DEFAULT_ANGLE_BAR_COUNT = 1;

    private final ClosePriceIndicatorM closeIndicator;

    private final int fastPeriod;
    private final int mediumPeriod;
    private final int slowPeriod;
    private final int angleBarCount;

    // No-args constructor required by CDI
    protected TripleMAIndicatorM() {
        super(null);
        this.closeIndicator = null;
        this.fastPeriod = DEFAULT_FAST_PERIOD;
        this.mediumPeriod = DEFAULT_MEDIUM_PERIOD;
        this.slowPeriod = DEFAULT_SLOW_PERIOD;
        this.angleBarCount = DEFAULT_ANGLE_BAR_COUNT;
    }

    @Inject
    public TripleMAIndicatorM(Candle candle, ClosePriceIndicatorM closeIndicator) {
        this(candle, closeIndicator, DEFAULT_FAST_PERIOD, DEFAULT_MEDIUM_PERIOD,
             DEFAULT_SLOW_PERIOD, DEFAULT_ANGLE_BAR_COUNT);
    }

    public TripleMAIndicatorM(Candle candle,
                             ClosePriceIndicatorM closeIndicator,
                             int fastPeriod,
                             int mediumPeriod,
                             int slowPeriod,
                             int angleBarCount) {
        super(candle);
        this.closeIndicator = closeIndicator;
        this.fastPeriod = fastPeriod;
        this.mediumPeriod = mediumPeriod;
        this.slowPeriod = slowPeriod;
        this.angleBarCount = angleBarCount;
    }

    @Override
    protected TripleMAIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new TripleMAIndicator(
            closeIndicator.getIndicator(timeframe, isLifeSeries),
            fastPeriod,
            mediumPeriod,
            slowPeriod,
            angleBarCount
        );
    }
}
