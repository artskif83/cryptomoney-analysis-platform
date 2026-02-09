package artskif.trader.strategy.indicators.base;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResistanceLevelIndicatorTest {

    @Test
    void getCountOfUnstableBars_shouldReturnZero() {
        BarSeries series = seriesWithHighs(100, 101, 102);
        HighPriceIndicator high = new HighPriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series);

        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, strength, 3, series.numFactory().numOf(0.10));

        assertEquals(0, indicator.getCountOfUnstableBars());
    }

    @Test
    void sortByHighPrice_shouldSortDescending_andPreserveOriginalIndex_andRespectLookbackWindow() {
        BarSeries series = seriesWithHighs(10, 50, 30, 40, 20);
        HighPriceIndicator high = new HighPriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series);

        // barCount=3 => для currentIndex=4 берём индексы [2..4]
        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, strength, 3, series.numFactory().numOf(0.10));

        List<ResistanceLevelIndicator.PriceWithIndex> sorted = indicator.sortByHighPrice(high, strength, 4);

        assertEquals(3, sorted.size());

        // highs: idx2=30, idx3=40, idx4=20 => sorted: 40(idx3),30(idx2),20(idx4)
        assertEquals(series.numFactory().numOf(40), sorted.get(0).getPrice());
        assertEquals(3, sorted.get(0).getOriginalIndex());

        assertEquals(series.numFactory().numOf(30), sorted.get(1).getPrice());
        assertEquals(2, sorted.get(1).getOriginalIndex());

        assertEquals(series.numFactory().numOf(20), sorted.get(2).getPrice());
        assertEquals(4, sorted.get(2).getOriginalIndex());

        // Плюс проверим, что у всех элементов заполнилась сила сопротивления (не null)
        assertTrue(sorted.stream().allMatch(p -> p.getResistanceStrength() != null));
    }

    @Test
    void resistancePower_shouldReturnZero_whenSortedPricesEmpty() {
        BarSeries series = seriesWithHighs(100);
        HighPriceIndicator high = new HighPriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series);
        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, strength, 32, series.numFactory().numOf(0.10));

        Num result = indicator.resistancePower(List.of(), series.numFactory().numOf(0.10), series.numFactory().numOf(100));

        assertEquals(series.numFactory().zero(), result);
    }

    @Test
    void resistancePower_shouldBeInclusiveOnBounds_andChooseMaximumWindowContainingCurrentPrice() {
        BarSeries series = seriesWithHighs(100); // нужен только numFactory()
        HighPriceIndicator high = new HighPriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series);
        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, strength, 32, series.numFactory().numOf(0.10));

        // threshold = 10%
        Num threshold = series.numFactory().numOf(0.10);

        // Цены отсортированы по убыванию (как в sortByHighPrice)
        // Окно для top=110: [99..110], в него попадают 110(str=2) и 100(str=5) => total=7
        // Окно для top=100: [90..100], currentPrice=99 попадает, туда 100(str=5) и 95(str=3) => total=8 (максимум)
        List<ResistanceLevelIndicator.PriceWithIndex> sorted = List.of(
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(110), 0, series.numFactory().numOf(2)),
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(100), 1, series.numFactory().numOf(5)),
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(95), 2, series.numFactory().numOf(3)),
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(80), 3, series.numFactory().numOf(100)) // должен быть за пределами окон
        );

        Num currentPrice = series.numFactory().numOf(99); // ровно на lowerBound для top=110
        Num result = indicator.resistancePower(sorted, threshold, currentPrice);

        assertEquals(series.numFactory().numOf(8), result);

        // Ещё проверим включённость верхней границы: currentPrice==topPrice
        Num resultAtTop = indicator.resistancePower(sorted, threshold, series.numFactory().numOf(100));
        assertEquals(series.numFactory().numOf(8), resultAtTop);
    }

    @Test
    void getValue_shouldComputeResistanceUsingCurrentHighPrice_asCurrentPrice() {
        // Подберём серию так, чтобы CandleResistanceStrength вернуло предсказуемые значения.
        // Мы целимся в то, чтобы у двух баров была сила=2, у одного сила=1.
        // CandleResistanceStrength даёт 2 для "красная без тени" после "зелёной без тени".
        // Тень считаем через high - max(open,close). Чтобы тени не было, high==max(open,close).

        BarSeries series = new BaseBarSeriesBuilder().withName("s").build();
        Instant t = Instant.parse("2025-12-23T10:00:00Z");

        // idx0: зелёная без тени => strength(0)=0 (index==begin)
        series.addBar(bar(t, 100, 110, 90, 110));

        // idx1: красная без тени, prev зелёная без тени => strength=2, high=120
        series.addBar(bar(t.plusSeconds(60), 120, 120, 100, 110));

        // idx2: красная без тени, prev красная без тени (prevShadow < thr, prevClose<prevOpen false) => strength=1, high=119
        series.addBar(bar(t.plusSeconds(120), 119, 119, 100, 110));

        // idx3: красная без тени, prev красная без тени -> strength=1, high=120 (такая же цена как idx1)
        series.addBar(bar(t.plusSeconds(180), 120, 120, 100, 110));

        HighPriceIndicator high = new HighPriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series, series.numFactory().numOf(0.03));

        // threshold 0.2% (0.002) как дефолт, barCount=32
        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, strength);

        // На idx3 currentHigh=120.
        // Окно top=120: lower=120*(1-0.002)=119.76, в него попадают high 120(idx1,str=2) и 120(idx3,str=1) => total=3.
        // high 119(idx2) ниже lowerBound => не попадает.
        Num value = indicator.getValue(3);
        assertEquals(series.numFactory().numOf(3), value);
    }

    private static BarSeries seriesWithHighs(double... highs) {
        BarSeries series = new BaseBarSeriesBuilder().withName("s").build();
        Instant t = Instant.parse("2025-01-01T00:00:00Z");
        for (int i = 0; i < highs.length; i++) {
            // open/close ставим равными high, чтобы "тень" была 0 и формулы не влияли
            double h = highs[i];
            series.addBar(bar(t.plusSeconds(i * 60L), h, h, h, h));
        }
        return series;
    }

    private static BaseBar bar(Instant endTime, double open, double high, double low, double close) {
        // В ta4j BaseBar есть конструктор (Duration, beginTime, endTime, ...)
        Instant beginTime = endTime.minus(Duration.ofMinutes(1));
        return new BaseBar(
                Duration.ofMinutes(1),
                beginTime,
                endTime,
                DecimalNum.valueOf(open),
                DecimalNum.valueOf(high),
                DecimalNum.valueOf(low),
                DecimalNum.valueOf(close),
                DecimalNum.valueOf(0),
                DecimalNum.valueOf(0),
                0L
        );
    }
}
