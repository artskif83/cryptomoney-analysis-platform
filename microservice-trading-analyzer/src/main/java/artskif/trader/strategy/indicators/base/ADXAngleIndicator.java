package artskif.trader.strategy.indicators.base;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.num.Num;

/**
 * Индикатор угла наклона кривой ADX.
 * <p>
 * Вычисляет угол наклона ADX в градусах на основе изменения его значения
 * за последние {@code angleBarCount} баров. Угол рассчитывается как:
 * <pre>
 *   slope = percentChange / angleBarCount
 *   angle = atan(slope) * 180 / PI
 * </pre>
 * где {@code percentChange} — процентное изменение ADX относительно предыдущего значения.
 * <p>
 * Положительное значение означает восходящий тренд (ADX растёт),
 * отрицательное — нисходящий (ADX падает).
 */
public class ADXAngleIndicator extends CachedIndicator<Num> {

    private final ADXIndicator adxIndicator;
    private final int angleBarCount;

    /**
     * Конструктор с периодом ADX по умолчанию (14) и окном угла 1 бар.
     *
     * @param barSeries серия баров
     */
    public ADXAngleIndicator(BarSeries barSeries) {
        this(barSeries, 14, 1);
    }

    /**
     * Конструктор с настраиваемыми параметрами.
     *
     * @param barSeries     серия баров
     * @param adxPeriod     период ADX (обычно 14)
     * @param angleBarCount количество баров для расчёта угла наклона (должно быть >= 1)
     */
    public ADXAngleIndicator(BarSeries barSeries, int adxPeriod, int angleBarCount) {
        super(barSeries);
        if (angleBarCount < 1) {
            throw new IllegalArgumentException("angleBarCount must be >= 1, got: " + angleBarCount);
        }
        this.adxIndicator = new ADXIndicator(barSeries, adxPeriod);
        this.angleBarCount = angleBarCount;
    }

    /**
     * Конструктор, принимающий уже созданный {@link ADXIndicator}.
     * Удобен, если ADX уже используется в стратегии и его не нужно создавать повторно.
     *
     * @param adxIndicator  готовый индикатор ADX
     * @param angleBarCount количество баров для расчёта угла наклона (должно быть >= 1)
     */
    public ADXAngleIndicator(ADXIndicator adxIndicator, int angleBarCount) {
        super(adxIndicator.getBarSeries());
        if (angleBarCount < 1) {
            throw new IllegalArgumentException("angleBarCount must be >= 1, got: " + angleBarCount);
        }
        this.adxIndicator = adxIndicator;
        this.angleBarCount = angleBarCount;
    }

    /**
     * Вычисляет угол наклона кривой ADX для бара с указанным индексом.
     *
     * @param index индекс текущего бара
     * @return угол наклона в градусах; 0, если данных недостаточно
     */
    @Override
    protected Num calculate(int index) {
        if (index < angleBarCount) {
            return getBarSeries().numFactory().zero();
        }

        Num currentValue = adxIndicator.getValue(index);
        Num previousValue = adxIndicator.getValue(index - angleBarCount);

        // Защита от деления на ноль
        if (previousValue.isZero()) {
            return getBarSeries().numFactory().zero();
        }

        // Процентное изменение ADX
        Num percentChange = currentValue.minus(previousValue)
                .dividedBy(previousValue)
                .multipliedBy(getBarSeries().numFactory().hundred());

        // Нормируем на количество баров и берём арктангенс → градусы
        double slope = percentChange.dividedBy(getBarSeries().numFactory().numOf(angleBarCount)).doubleValue();
        double angleDegrees = Math.toDegrees(Math.atan(slope));

        return getBarSeries().numFactory().numOf(angleDegrees);
    }

    /**
     * Возвращает угол наклона ADX для заданного индекса.
     * Удобный псевдоним для {@link #getValue(int)}.
     *
     * @param index индекс бара
     * @return угол наклона в градусах
     */
    public Num getAngle(int index) {
        return getValue(index);
    }

    /**
     * Возвращает {@code true}, если ADX растёт (угол > 0°) на данном баре.
     *
     * @param index индекс бара
     * @return признак восходящего наклона ADX
     */
    public boolean isRising(int index) {
        return getValue(index).isGreaterThan(getBarSeries().numFactory().zero());
    }

    /**
     * Возвращает {@code true}, если ADX падает (угол < 0°) на данном баре.
     *
     * @param index индекс бара
     * @return признак нисходящего наклона ADX
     */
    public boolean isFalling(int index) {
        return getValue(index).isLessThan(getBarSeries().numFactory().zero());
    }

    @Override
    public int getCountOfUnstableBars() {
        return adxIndicator.getCountOfUnstableBars() + angleBarCount;
    }
}
