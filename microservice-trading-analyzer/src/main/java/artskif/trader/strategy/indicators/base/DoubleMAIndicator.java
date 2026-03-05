package artskif.trader.strategy.indicators.base;

import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;


/**
 * Индикатор тройной скользящей средней с расчетом углов наклона.
 * Содержит три SMA с разными периодами и вычисляет угол наклона для каждой из них.
 */
public class DoubleMAIndicator extends CachedIndicator<Num> {

    private final SMAIndicator fastSMA;
    private final SMAIndicator mediumSMA;

    private final int angleBarCount;

    /**
     * Конструктор с параметрами по умолчанию.
     *
     * @param closePriceIndicator индикатор цены закрытия
     */
    public DoubleMAIndicator(ClosePriceIndicator closePriceIndicator) {
        this(closePriceIndicator, 3, 6, 1);
    }

    /**
     * Конструктор с настраиваемыми параметрами.
     *
     * @param closePriceIndicator индикатор цены закрытия
     * @param fastPeriod          период быстрой SMA
     * @param mediumPeriod        период средней SMA
     * @param angleBarCount       количество баров для расчета угла наклона
     */
    public DoubleMAIndicator(ClosePriceIndicator closePriceIndicator,
                             int fastPeriod,
                             int mediumPeriod,
                             int angleBarCount) {
        super(closePriceIndicator);
        this.angleBarCount = angleBarCount;

        this.fastSMA = new SMAIndicator(closePriceIndicator, fastPeriod);
        this.mediumSMA = new SMAIndicator(closePriceIndicator, mediumPeriod);
    }

    @Override
    protected Num calculate(int index) {
        Num fastSMAAngle = getFastSMAAngle(index);

        if (getFastSMA(index).isGreaterThan(getMediumSMA(index)) && fastSMAAngle.isGreaterThan(getBarSeries().numFactory().zero())) {
            return getBarSeries().numFactory().one();
        } else if (getFastSMA(index).isLessThan(getMediumSMA(index)) && fastSMAAngle.isLessThan(getBarSeries().numFactory().zero())) {
            return getBarSeries().numFactory().minusOne();
        } else {
            return getBarSeries().numFactory().zero();
        }
    }

    /**
     * Получить значение быстрой скользящей средней.
     *
     * @param index индекс бара
     * @return значение быстрой SMA
     */
    public Num getFastSMA(int index) {
        return fastSMA.getValue(index);
    }

    /**
     * Получить значение средней скользящей средней.
     *
     * @param index индекс бара
     * @return значение средней SMA
     */
    public Num getMediumSMA(int index) {
        return mediumSMA.getValue(index);
    }

    /**
     * Вычисляет угол наклона для указанной SMA в градусах.
     * Угол рассчитывается на основе изменения значения SMA за последние angleBarCount баров.
     *
     * @param smaIndicator индикатор SMA
     * @param index        текущий индекс бара
     * @return угол наклона в градусах (положительный - восходящий тренд, отрицательный - нисходящий)
     */
    private Num calculateAngle(SMAIndicator smaIndicator, int index) {
        if (index < angleBarCount) {
            return getBarSeries().numFactory().zero();
        }

        Num currentValue = smaIndicator.getValue(index);
        Num previousValue = smaIndicator.getValue(index - angleBarCount);

        // Вычисляем изменение значения
        Num delta = currentValue.minus(previousValue);

        // Нормализуем по цене для получения процентного изменения
        Num percentChange = delta.dividedBy(previousValue).multipliedBy(getBarSeries().numFactory().hundred());

        // Вычисляем угол: arctg(percentChange / angleBarCount) * 180 / PI
        // Для упрощения используем приближенное значение: percentChange / angleBarCount * 45
        // где 45 - это коэффициент для перевода в градусы (можно настроить)
        Num slope = percentChange.dividedBy(getBarSeries().numFactory().numOf(angleBarCount));

        // Преобразуем в градусы (упрощенная формула)
        // atan(slope) ≈ slope для малых углов, умножаем на 57.2958 для перевода в градусы
        double slopeValue = slope.doubleValue();
        double angleRadians = Math.atan(slopeValue);
        double angleDegrees = Math.toDegrees(angleRadians);

        return getBarSeries().numFactory().numOf(angleDegrees);
    }

    /**
     * Получить угол наклона быстрой скользящей средней.
     *
     * @param index индекс бара
     * @return угол наклона в градусах
     */
    public Num getFastSMAAngle(int index) {
        return calculateAngle(fastSMA, index);
    }

    /**
     * Получить угол наклона средней скользящей средней.
     *
     * @param index индекс бара
     * @return угол наклона в градусах
     */
    public Num getMediumSMAAngle(int index) {
        return calculateAngle(mediumSMA, index);
    }


    @Override
    public int getCountOfUnstableBars() {
        return 27;
    }
}
