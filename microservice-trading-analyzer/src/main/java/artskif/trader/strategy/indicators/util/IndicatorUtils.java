package artskif.trader.strategy.indicators.util;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.time.Duration;
import java.time.Instant;

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
}
