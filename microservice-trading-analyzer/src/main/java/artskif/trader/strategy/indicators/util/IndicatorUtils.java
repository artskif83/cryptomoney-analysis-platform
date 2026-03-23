package artskif.trader.strategy.indicators.util;

import artskif.trader.strategy.indicators.base.PriceWithIndex;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class IndicatorUtils {

    /**
     * Маппинг индекса свечи на нижнем таймфрейме к индексу свечи на старшем таймфрейме.
     *
     * <p>Старшая свеча считается «применимой», только если она уже <b>закрылась</b> до момента
     * окончания младшей свечи. Проверяется, что время окончания младшей свечи {@code t}
     * попадает в полуоткрытый интервал {@code (endTime, endTime + duration]} старшей свечи:
     * <pre>
     *   hBar.endTime &lt; t  &amp;&amp;  t &lt;= hBar.endTime + hBar.timePeriod
     * </pre>
     * Это гарантирует отсутствие «заглядывания в будущее»: пока младшая свеча формируется
     * внутри старшей, последняя ещё не закрыта и не используется.
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
            Duration duration = hBar.getTimePeriod();
            Instant hEnd = hBar.getEndTime();

            // Старшая свеча уже закрылась (hEnd < t) и следующий её период содержит t (t <= hEnd + duration)
            if (hEnd.isBefore(t) && !t.isAfter(hEnd.plus(duration))) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Сортирует элементы по значениям индикатора lowPriceIndicator по возрастанию (от минимальной к максимальной),
     * сохраняя исходные индексы.
     *
     * @param lowPriceIndicator индикатор минимальной цены
     * @param lowBarCount       количество баров для анализа
     * @param currentIndex      текущий индекс
     * @return отсортированный список элементов с сохранёнными индексами
     */
    public static List<PriceWithIndex> sortByLowPrice(LowPriceIndicator lowPriceIndicator,
                                                      int lowBarCount,
                                                      int currentIndex) {
        List<PriceWithIndex> pricesWithIndices = new ArrayList<>();

        int startIndex = Math.max(0, currentIndex - lowBarCount + 1);

        for (int i = startIndex; i <= currentIndex; i++) {
            Num lowPrice = lowPriceIndicator.getValue(i);
            pricesWithIndices.add(new PriceWithIndex(lowPrice, i));
        }

        // Сортируем по возрастанию цены (от минимальной к максимальной)
        pricesWithIndices.sort(Comparator.comparing(PriceWithIndex::getPrice));

        return pricesWithIndices;
    }

    /**
     * Сортирует элементы по значениям индикатора highPriceIndicator по убыванию (от максимальной к минимальной),
     * сохраняя исходные индексы.
     *
     * @param highPriceIndicator индикатор максимальной цены
     * @param highBarCount       количество баров для анализа
     * @param currentIndex       текущий индекс
     * @return отсортированный список элементов с сохранёнными индексами
     */
    public static List<PriceWithIndex> sortByHighPrice(HighPriceIndicator highPriceIndicator,
                                                       int highBarCount,
                                                       int currentIndex) {
        List<PriceWithIndex> pricesWithIndices = new ArrayList<>();

        int startIndex = Math.max(0, currentIndex - highBarCount + 1);

        for (int i = startIndex; i <= currentIndex; i++) {
            Num highPrice = highPriceIndicator.getValue(i);
            pricesWithIndices.add(new PriceWithIndex(highPrice, i));
        }

        // Сортируем по убыванию цены (от максимальной к минимальной)
        pricesWithIndices.sort(Comparator.comparing(PriceWithIndex::getPrice).reversed());

        return pricesWithIndices;
    }
}
