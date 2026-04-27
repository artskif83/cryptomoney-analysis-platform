package artskif.trader.strategy.indicators.base;

import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;


/**
 * Индикатор скользящей средней с расчетом углов наклона.
 * Содержит 5 SMA с разными периодами и вычисляет угол наклона для каждой из них.
 * Метод calculate возвращает сумму сигналов по углам от -5 до +5.
 */
public class MultiMAIndicator extends CachedIndicator<Num> {

    private final SMAIndicator sma1;
    private final SMAIndicator sma2;
    private final SMAIndicator sma3;
    private final SMAIndicator sma4;
    private final SMAIndicator sma5;

    private final int angleBarCount;

    /**
     * Конструктор с параметрами по умолчанию (периоды: 10, 20, 50, 100, 200).
     *
     * @param closePriceIndicator индикатор цены закрытия
     */
    public MultiMAIndicator(ClosePriceIndicator closePriceIndicator) {
        this(closePriceIndicator, 10, 20, 50, 100, 200, 1);
    }

    /**
     * Конструктор с настраиваемыми параметрами.
     *
     * @param closePriceIndicator индикатор цены закрытия
     * @param period1             период SMA1
     * @param period2             период SMA2
     * @param period3             период SMA3
     * @param period4             период SMA4
     * @param period5             период SMA5
     * @param angleBarCount       количество баров для расчета угла наклона
     */
    public MultiMAIndicator(ClosePriceIndicator closePriceIndicator,
                             int period1,
                             int period2,
                             int period3,
                             int period4,
                             int period5,
                             int angleBarCount) {
        super(closePriceIndicator);
        this.angleBarCount = angleBarCount;

        this.sma1 = new SMAIndicator(closePriceIndicator, period1);
        this.sma2 = new SMAIndicator(closePriceIndicator, period2);
        this.sma3 = new SMAIndicator(closePriceIndicator, period3);
        this.sma4 = new SMAIndicator(closePriceIndicator, period4);
        this.sma5 = new SMAIndicator(closePriceIndicator, period5);
    }

    @Override
    protected Num calculate(int index) {
        int score = 0;
        Num zero = getBarSeries().numFactory().zero();

        if (getSma1Angle(index).isGreaterThan(zero)) score++; else score--;
        if (getSma2Angle(index).isGreaterThan(zero)) score++; else score--;
        if (getSma3Angle(index).isGreaterThan(zero)) score++; else score--;
        if (getSma4Angle(index).isGreaterThan(zero)) score++; else score--;
        if (getSma5Angle(index).isGreaterThan(zero)) score++; else score--;

        return getBarSeries().numFactory().numOf(score);
    }

    /**
     * Получить значение SMA1.
     *
     * @param index индекс бара
     * @return значение SMA1
     */
    public Num getSma1(int index) { return sma1.getValue(index); }

    /**
     * Получить значение SMA2.
     *
     * @param index индекс бара
     * @return значение SMA2
     */
    public Num getSma2(int index) { return sma2.getValue(index); }

    /**
     * Получить значение SMA3.
     *
     * @param index индекс бара
     * @return значение SMA3
     */
    public Num getSma3(int index) { return sma3.getValue(index); }

    /**
     * Получить значение SMA4.
     *
     * @param index индекс бара
     * @return значение SMA4
     */
    public Num getSma4(int index) { return sma4.getValue(index); }

    /**
     * Получить значение SMA5.
     *
     * @param index индекс бара
     * @return значение SMA5
     */
    public Num getSma5(int index) { return sma5.getValue(index); }

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
     * Получить угол наклона SMA1.
     *
     * @param index индекс бара
     * @return угол наклона в градусах
     */
    public Num getSma1Angle(int index) { return calculateAngle(sma1, index); }

    /**
     * Получить угол наклона SMA2.
     *
     * @param index индекс бара
     * @return угол наклона в градусах
     */
    public Num getSma2Angle(int index) { return calculateAngle(sma2, index); }

    /**
     * Получить угол наклона SMA3.
     *
     * @param index индекс бара
     * @return угол наклона в градусах
     */
    public Num getSma3Angle(int index) { return calculateAngle(sma3, index); }

    /**
     * Получить угол наклона SMA4.
     *
     * @param index индекс бара
     * @return угол наклона в градусах
     */
    public Num getSma4Angle(int index) { return calculateAngle(sma4, index); }

    /**
     * Получить угол наклона SMA5.
     *
     * @param index индекс бара
     * @return угол наклона в градусах
     */
    public Num getSma5Angle(int index) { return calculateAngle(sma5, index); }


    @Override
    public int getCountOfUnstableBars() {
        return 200;
    }
}


