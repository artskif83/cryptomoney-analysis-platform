package artskif.trader.strategy.indicators.base;

import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;


/**
 * Индикатор тройной скользящей средней с расчетом углов наклона.
 * Содержит три SMA с разными периодами и вычисляет угол наклона для каждой из них.
 */
public class TripleMAIndicator extends CachedIndicator<Num> {

    private final SMAIndicator fastSMA;
    private final SMAIndicator mediumSMA;
    private final SMAIndicator slowSMA;

    private final int fastPeriod;
    private final int mediumPeriod;
    private final int slowPeriod;
    private final int angleBarCount;

    /**
     * Конструктор с параметрами по умолчанию.
     *
     * @param closePriceIndicator индикатор цены закрытия
     */
    public TripleMAIndicator(ClosePriceIndicator closePriceIndicator) {
        this(closePriceIndicator, 3, 9, 27, 1);
    }

    /**
     * Конструктор с настраиваемыми параметрами.
     *
     * @param closePriceIndicator индикатор цены закрытия
     * @param fastPeriod период быстрой SMA
     * @param mediumPeriod период средней SMA
     * @param slowPeriod период медленной SMA
     * @param angleBarCount количество баров для расчета угла наклона
     */
    public TripleMAIndicator(ClosePriceIndicator closePriceIndicator,
                            int fastPeriod,
                            int mediumPeriod,
                            int slowPeriod,
                            int angleBarCount) {
        super(closePriceIndicator);
        this.fastPeriod = fastPeriod;
        this.mediumPeriod = mediumPeriod;
        this.slowPeriod = slowPeriod;
        this.angleBarCount = angleBarCount;

        this.fastSMA = new SMAIndicator(closePriceIndicator, fastPeriod);
        this.mediumSMA = new SMAIndicator(closePriceIndicator, mediumPeriod);
        this.slowSMA = new SMAIndicator(closePriceIndicator, slowPeriod);
    }

    @Override
    protected Num calculate(int index) {
        Num rate = getBarSeries().numFactory().zero();
        if (isBullishAlignment(index)) {
            rate = rate.plus(getBarSeries().numFactory().one());
        } else if (isBearishAlignment(index)) {
            rate = rate.minus(getBarSeries().numFactory().one());
        }
        if (areAllAnglesPositive(index)) {
            rate = rate.plus(getBarSeries().numFactory().one());
        } else if (areAllAnglesNegative(index)) {
            rate = rate.minus(getBarSeries().numFactory().one());
        }
        // Возвращаем значение быстрой SMA как основное значение индикатора
        return rate;
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
     * Получить значение медленной скользящей средней.
     *
     * @param index индекс бара
     * @return значение медленной SMA
     */
    public Num getSlowSMA(int index) {
        return slowSMA.getValue(index);
    }

    /**
     * Вычисляет угол наклона для указанной SMA в градусах.
     * Угол рассчитывается на основе изменения значения SMA за последние angleBarCount баров.
     *
     * @param smaIndicator индикатор SMA
     * @param index текущий индекс бара
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

    /**
     * Получить угол наклона медленной скользящей средней.
     *
     * @param index индекс бара
     * @return угол наклона в градусах
     */
    public Num getSlowSMAAngle(int index) {
        return calculateAngle(slowSMA, index);
    }

    /**
     * Проверяет, выстроены ли все SMA в правильном порядке (бычий тренд).
     * Fast > Medium > Slow
     *
     * @param index индекс бара
     * @return true, если выстроены в бычьем порядке
     */
    public boolean isBullishAlignment(int index) {
        Num fast = getFastSMA(index);
        Num medium = getMediumSMA(index);
        Num slow = getSlowSMA(index);

        return fast.isGreaterThan(medium) && medium.isGreaterThan(slow);
    }

    /**
     * Проверяет, выстроены ли все SMA в правильном порядке (медвежий тренд).
     * Fast < Medium < Slow
     *
     * @param index индекс бара
     * @return true, если выстроены в медвежьем порядке
     */
    public boolean isBearishAlignment(int index) {
        Num fast = getFastSMA(index);
        Num medium = getMediumSMA(index);
        Num slow = getSlowSMA(index);

        return fast.isLessThan(medium) && medium.isLessThan(slow);
    }

    /**
     * Проверяет, направлены ли все углы наклона вверх (все SMA растут).
     *
     * @param index индекс бара
     * @return true, если все углы положительные
     */
    public boolean areAllAnglesPositive(int index) {
        return getFastSMAAngle(index).isPositive()
            && getMediumSMAAngle(index).isPositive()
            && getSlowSMAAngle(index).isPositive();
    }

    /**
     * Проверяет, направлены ли все углы наклона вниз (все SMA падают).
     *
     * @param index индекс бара
     * @return true, если все углы отрицательные
     */
    public boolean areAllAnglesNegative(int index) {
        return getFastSMAAngle(index).isNegative()
            && getMediumSMAAngle(index).isNegative()
            && getSlowSMAAngle(index).isNegative();
    }

    @Override
    public int getCountOfUnstableBars() {
        return 27;
    }
}
