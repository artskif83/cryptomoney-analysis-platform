package artskif.trader.strategy.indicators.multi.levels;

 import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import artskif.trader.strategy.indicators.base.ShortHighLevelIndicator;
import artskif.trader.strategy.indicators.multi.ClosePriceIndicatorM;
import artskif.trader.strategy.indicators.multi.LowPriceIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;

/**
 * Мульти-таймфреймовый индикатор уровня сопротивления для шорта (высший таймфрейм).
 * Оборачивает {@link ShortHighLevelIndicator} и позволяет использовать его
 * на разных таймфреймах через CDI.
 */
@ApplicationScoped
public class ShortHighLevelIndicatorM extends MultiAbstractIndicator<ShortHighLevelIndicator> {

    /** Количество баров для определения уровня сопротивления высшего таймфрейма */
    public static final int DEFAULT_HIGH_BAR_COUNT = 5;

    /** Допустимый процент отклонения для определения зоны уровня */
    public static final double DEFAULT_HIGH_THRESHOLD_PERCENTAGES = 0.1;

    /** Радиус окна для определения точки входа */
    public static final double DEFAULT_CALCULATION_RADIUS_PERCENTAGES = 0.2;

    private final LowPriceIndicatorM lowPriceIndicatorM;
    private final ClosePriceIndicatorM closePriceIndicatorM;

    // No-args constructor required by CDI
    protected ShortHighLevelIndicatorM() {
        super(null);
        this.lowPriceIndicatorM = null;
        this.closePriceIndicatorM = null;
    }

    @Inject
    public ShortHighLevelIndicatorM(Candle candle,
                                    LowPriceIndicatorM lowPriceIndicatorM,
                                    ClosePriceIndicatorM closePriceIndicatorM) {
        super(candle);
        this.lowPriceIndicatorM = lowPriceIndicatorM;
        this.closePriceIndicatorM = closePriceIndicatorM;
    }

    @Override
    protected ShortHighLevelIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new ShortHighLevelIndicator(
                lowPriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                closePriceIndicatorM.getIndicator(timeframe, isLifeSeries),
                DEFAULT_HIGH_BAR_COUNT,
                DecimalNum.valueOf(DEFAULT_HIGH_THRESHOLD_PERCENTAGES),
                DecimalNum.valueOf(DEFAULT_CALCULATION_RADIUS_PERCENTAGES)
        );
    }
}
