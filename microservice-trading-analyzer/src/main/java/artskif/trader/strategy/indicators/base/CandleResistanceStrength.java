package artskif.trader.strategy.indicators.base;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class CandleResistanceStrength extends CachedIndicator<Num> {

    private final Num shadowPercentThreshold;

    public CandleResistanceStrength(BarSeries series) {
        this(series, series.numFactory().numOf(0.03)); // Default threshold of 0.03%
    }

    public CandleResistanceStrength(BarSeries series, Num shadowPercentThreshold) {
        super(series);
        this.shadowPercentThreshold = shadowPercentThreshold;
    }

    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }

    @Override
    protected Num calculate(int index) {
        if (index <= getBarSeries().getBeginIndex()) {
            return getBarSeries().numFactory().zero();
        }

        Num currentClose = getBarSeries().getBar(index).getClosePrice();
        Num currentOpen = getBarSeries().getBar(index).getOpenPrice();
        Num currentHigh = getBarSeries().getBar(index).getHighPrice();

        Num previousClose = getBarSeries().getBar(index-1).getClosePrice();
        Num previousOpen = getBarSeries().getBar(index-1).getOpenPrice();
        Num previousHigh = getBarSeries().getBar(index-1).getHighPrice();

        Num currentShadowPercent = calculatePercentageChange(currentHigh, currentClose.max(currentOpen));
        Num previousShadowPercent = calculatePercentageChange(previousHigh, previousClose.max(previousOpen));

        if (currentShadowPercent.isLessThan(shadowPercentThreshold)) {
            // Тень от текущей свечи слишком короткая. Смотрим предыдщую свечу.
            if (currentClose.isGreaterThan(currentOpen)) {
                // Зеленая свеча без тени - значит ранг 0, неважно какая была предыдущая свеча
                return getBarSeries().numFactory().zero();
            } else if (previousShadowPercent.isLessThan(shadowPercentThreshold) && previousClose.isGreaterThan(previousOpen)) {
                // Красная без тени, предыдущая свеча зеленая без тени значит ранг сопротивления 2
                return getBarSeries().numFactory().two();
            } else {
                // Красная без тени, предыдущая свеча зеленая или красная с тенью, значит ранг 1
                return getBarSeries().numFactory().one();
            }
        } else {
            if (currentClose.isGreaterThan(currentOpen)) {
                // Зеленая свеча с длинной тенью, значит ранг 1
                return getBarSeries().numFactory().one();
            } else if (previousShadowPercent.isLessThan(shadowPercentThreshold) && previousClose.isGreaterThan(previousOpen)) {
                // Красная с длинной тенью, предыдущая свеча зеленая без тени значит ранг сопротивления 3
                return getBarSeries().numFactory().three();
            } else {
                // Красная с длинной тенью, предыдущая свеча зеленая с тенью, значит ранг 2
                return getBarSeries().numFactory().two();
            }
        }
    }

    protected Num calculatePercentageChange(Num currentValue, Num previousValue) {
        // Rate of return: ((current - previous) / previous) * 100
        // Equivalent to: (current / previous - 1) * 100
        Num change = currentValue.minus(previousValue);
        Num changeFraction = change.dividedBy(previousValue);
        return changeFraction.multipliedBy(getBarSeries().numFactory().hundred());
    }
}
