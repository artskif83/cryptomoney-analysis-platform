package artskif.trader.contract.util;

import artskif.trader.entity.ContractMetadata;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.time.Instant;
import java.time.ZonedDateTime;

public final class FeaturesUtils {

    /**
     * Маппинг индекса свечи на нижнем таймфрейме к индексу свечи на старшем таймфрейме
     *
     * @param lowerTfBar   свеча на нижнем таймфрейме
     * @param higherSeries серия свечей на старшем таймфрейме
     * @return индекс свечи на старшем таймфрейме
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

        throw new IllegalStateException(
                "No higher timeframe bar found for time: " + t
        );
    }
}
