package artskif.trader.contract.util;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.time.Instant;

public final class FeaturesUtils {

    /**
     * Маппинг индекса свечи на нижнем таймфрейме к индексу свечи на старшем таймфрейме
     *
     * @param lowerTfBar   свеча на нижнем таймфрейме
     * @param higherSeries серия свечей на старшем таймфрейме
     * @return индекс свечи на старшем таймфрейме или -1, если соответствующая свеча не найдена
     */
    public static int mapToHigherTfIndex(
            Bar lowerTfBar,
            BarSeries higherSeries
    ) {
        Instant t = lowerTfBar.getEndTime();

        for (int i = higherSeries.getEndIndex(); i >= higherSeries.getBeginIndex(); i--) {
            Bar hBar = higherSeries.getBar(i);

            if (!hBar.getEndTime().isAfter(t)) {
                return i;
            }
        }

        return -1;
    }
}
