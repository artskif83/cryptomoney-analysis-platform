package artskif.trader.strategy.indicators.multi.levels;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.LongHighLevelIndicator;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.HighPriceIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;

/**
 * Мульти-таймфреймовый индикатор уровня сопротивления для лонга (высший таймфрейм).
 * Оборачивает {@link LongHighLevelIndicator} и позволяет использовать его
 * на разных таймфреймах через CDI.
 */
@ApplicationScoped
public class LongHighLevelIndicatorM extends MultiAbstractIndicator<LongHighLevelIndicator> {

    /** Количество баров для определения уровня сопротивления высшего таймфрейма */
    public static final int DEFAULT_HIGH_BAR_COUNT = 5;

    /** Допустимый процент отклонения для определения зоны уровня */
    public static final double DEFAULT_HIGH_THRESHOLD_PERCENTAGES = 0.1;

    /** Радиус окна для определения точки входа */
    public static final double DEFAULT_CALCULATION_RADIUS_PERCENTAGES = 0.2;

    private final HighPriceIndicatorM highPriceIndicatorM;
    private final ClosePriceIndicatorM closePriceIndicatorM;

    // No-args constructor required by CDI
    protected LongHighLevelIndicatorM() {
        super(null);
        this.highPriceIndicatorM = null;
        this.closePriceIndicatorM = null;
    }

    @Inject
    public LongHighLevelIndicatorM(Candle candle,
                                   HighPriceIndicatorM highPriceIndicatorM,
                                   ClosePriceIndicatorM closePriceIndicatorM) {
        super(candle);
        this.highPriceIndicatorM = highPriceIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
    }

    @Override
    protected LongHighLevelIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new LongHighLevelIndicator(
                highPriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                DEFAULT_HIGH_BAR_COUNT,
                DecimalNum.valueOf(DEFAULT_HIGH_THRESHOLD_PERCENTAGES),
                DecimalNum.valueOf(DEFAULT_CALCULATION_RADIUS_PERCENTAGES)
        );
    }
}
