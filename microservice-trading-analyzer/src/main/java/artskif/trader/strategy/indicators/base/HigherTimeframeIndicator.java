package artskif.trader.strategy.indicators.base;

import artskif.trader.strategy.indicators.util.IndicatorUtils;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.NaN;
import org.ta4j.core.num.Num;

public class HigherTimeframeIndicator  extends CachedIndicator<Num> {

    private final Indicator<Num> lowerTfIndicator;
    private final Indicator<Num> higherTfIndicator;

    public HigherTimeframeIndicator(Indicator<Num> lowerTfIndicator, Indicator<Num> higherTfIndicator) {
        super(lowerTfIndicator);
        this.lowerTfIndicator = lowerTfIndicator;
        this.higherTfIndicator = higherTfIndicator;
    }

    @Override
    protected Num calculate(int index) {
        int higherTfIndex = IndicatorUtils.mapToHigherTfIndex(lowerTfIndicator.getBarSeries().getBar(index), higherTfIndicator.getBarSeries());
        return higherTfIndex == -1 ? NaN.NaN : higherTfIndicator.getValue(higherTfIndex);
    }

    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }
}
