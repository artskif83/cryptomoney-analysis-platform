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
public class DoubleMAIndicatorM extends MultiAbstractIndicator<MultiMAIndicator> {

    public static final int DEFAULT_FAST_PERIOD = 3;
    public static final int DEFAULT_MEDIUM_PERIOD = 6;
    public static final int DEFAULT_ANGLE_BAR_COUNT = 1;

    private final ClosePriceIndicatorM closeIndicator;

    private final int fastPeriod;
    private final int mediumPeriod;
    private final int angleBarCount;

    // No-args constructor required by CDI
    protected DoubleMAIndicatorM() {
        super(null);
        this.closeIndicator = null;
        this.fastPeriod = DEFAULT_FAST_PERIOD;
        this.mediumPeriod = DEFAULT_MEDIUM_PERIOD;
        this.angleBarCount = DEFAULT_ANGLE_BAR_COUNT;
    }

    @Inject
    public DoubleMAIndicatorM(Candle candle, ClosePriceIndicatorM closeIndicator) {
        this(candle, closeIndicator, DEFAULT_FAST_PERIOD, DEFAULT_MEDIUM_PERIOD, DEFAULT_ANGLE_BAR_COUNT);
    }

    public DoubleMAIndicatorM(Candle candle,
                              ClosePriceIndicatorM closeIndicator,
                              int fastPeriod,
                              int mediumPeriod,
                              int angleBarCount) {
        super(candle);
        this.closeIndicator = closeIndicator;
        this.fastPeriod = fastPeriod;
        this.mediumPeriod = mediumPeriod;
        this.angleBarCount = angleBarCount;
    }

    @Override
    protected MultiMAIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new MultiMAIndicator(
            closeIndicator.getIndicator(timeframe, isLifeSeries),
            fastPeriod,
            mediumPeriod,
            angleBarCount
        );
    }
}
