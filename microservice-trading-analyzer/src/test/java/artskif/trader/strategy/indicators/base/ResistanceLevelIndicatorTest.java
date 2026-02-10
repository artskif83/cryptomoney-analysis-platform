package artskif.trader.strategy.indicators.base;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
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
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series);

        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, close, strength, 3,
                series.numFactory().numOf(0.002), series.numFactory().numOf(0.10));

        assertEquals(0, indicator.getCountOfUnstableBars());
    }

    @Test
    void sortByHighPrice_shouldSortDescending_andPreserveOriginalIndex_andRespectLookbackWindow() {
        BarSeries series = seriesWithHighs(10, 50, 30, 40, 20);
        HighPriceIndicator high = new HighPriceIndicator(series);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series);

        // barCount=3 => для currentIndex=4 берём индексы [2..3] (текущий индекс 4 исключается)
        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, close, strength, 3,
                series.numFactory().numOf(0.002), series.numFactory().numOf(0.10));

        List<ResistanceLevelIndicator.PriceWithIndex> sorted = indicator.sortByHighPrice(high, strength, 4);

        assertEquals(2, sorted.size());

        // highs: idx2=30, idx3=40 (idx4=20 исключается) => sorted: 40(idx3),30(idx2)
        assertEquals(series.numFactory().numOf(40), sorted.get(0).getPrice());
        assertEquals(3, sorted.get(0).getOriginalIndex());

        assertEquals(series.numFactory().numOf(30), sorted.get(1).getPrice());
        assertEquals(2, sorted.get(1).getOriginalIndex());


        // Плюс проверим, что у всех элементов заполнилась сила сопротивления (не null)
        assertTrue(sorted.stream().allMatch(p -> p.getResistanceStrength() != null));
    }

    @Test
    void resistancePower_shouldReturnZeroResult_whenSortedPricesEmpty() {
        BarSeries series = seriesWithHighs(100);
        HighPriceIndicator high = new HighPriceIndicator(series);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series);
        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, close, strength, 32, series.numFactory().numOf(0.002), series.numFactory().numOf(0.10));

        ResistanceLevelIndicator.ResistanceWindowResult result = indicator.resistancePower(
                List.of(), series.numFactory().numOf(0.002), series.numFactory().numOf(100));

        assertEquals(series.numFactory().zero(), result.getMaxResistancePower());
        assertEquals(series.numFactory().zero(), result.getTopPrice());
        assertEquals(series.numFactory().zero(), result.getBottomPrice());
        assertEquals(series.numFactory().zero(), result.getResistancePowerAbove());
        assertEquals(series.numFactory().zero(), result.getResistancePowerBelow());
    }

    @Test
    void resistancePower_shouldFilterByZone_andChooseMaximumWindow_andCalculateAboveBelowSums() {
        BarSeries series = seriesWithHighs(100); // нужен только numFactory()
        HighPriceIndicator high = new HighPriceIndicator(series);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series);

        // resistanceRangePercentagesThreshold = 2% (размер окна)
        // resistanceZonePercentagesThreshold = 10% (фильтр по зоне вокруг currentPrice)
        Num rangeThreshold = series.numFactory().numOf(0.02);
        Num zoneThreshold = series.numFactory().numOf(0.10);

        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, close, strength, 32, rangeThreshold, zoneThreshold);

        // currentPrice = 100
        // Зона фильтрации: [100*(1-0.10)..100*(1+0.10)] = [90..110]
        // Цены отсортированы по убыванию
        List<ResistanceLevelIndicator.PriceWithIndex> sorted = List.of(
                // Цена 120 вне зоны (>110), должна быть отфильтрована
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(120), 0, series.numFactory().numOf(10)),
                // Цена 105 в зоне
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(105), 1, series.numFactory().numOf(2)),
                // Цена 100 в зоне
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(100), 2, series.numFactory().numOf(5)),
                // Цена 98 в зоне
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(98), 3, series.numFactory().numOf(3)),
                // Цена 95 в зоне
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(95), 4, series.numFactory().numOf(4)),
                // Цена 85 вне зоны (<90), должна быть отфильтрована
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(85), 5, series.numFactory().numOf(20))
        );

        Num currentPrice = series.numFactory().numOf(100);

        // После фильтрации остаются: 105(str=2), 100(str=5), 98(str=3), 95(str=4)
        // Проверяем окна (rangeThreshold=2%):
        // 1. Окно с top=105: lower=105*0.98=102.9, currentPrice=100 не попадает
        // 2. Окно с top=100: lower=100*0.98=98, currentPrice=100 попадает
        //    В окно [98..100] попадают: 100(str=5), 98(str=3) => total=8
        // 3. Окно с top=98: lower=98*0.98=96.04, currentPrice=100 не попадает (>98)
        // 4. Окно с top=95: lower=95*0.98=93.1, currentPrice=100 не попадает
        // Максимум: 8, top=100, bottom=98
        // Above (>100): 105(str=2) => 2
        // Below (<98): 95(str=4) => 4

        ResistanceLevelIndicator.ResistanceWindowResult result = indicator.resistancePower(sorted, rangeThreshold, currentPrice);

        assertEquals(series.numFactory().numOf(8), result.getMaxResistancePower());
        assertEquals(series.numFactory().numOf(100), result.getTopPrice());
        assertEquals(series.numFactory().numOf(98), result.getBottomPrice());
        assertEquals(series.numFactory().numOf(2), result.getResistancePowerAbove());
        assertEquals(series.numFactory().numOf(4), result.getResistancePowerBelow());
    }

    @Test
    void resistancePower_shouldReturnZeroResult_whenAllPricesOutsideZone() {
        BarSeries series = seriesWithHighs(100);
        HighPriceIndicator high = new HighPriceIndicator(series);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series);

        Num rangeThreshold = series.numFactory().numOf(0.02);
        Num zoneThreshold = series.numFactory().numOf(0.05); // 5%

        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, close, strength, 32, rangeThreshold, zoneThreshold);

        // currentPrice = 100, zone = [95..105]
        // Все цены вне зоны
        List<ResistanceLevelIndicator.PriceWithIndex> sorted = List.of(
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(120), 0, series.numFactory().numOf(10)),
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(80), 1, series.numFactory().numOf(5))
        );

        Num currentPrice = series.numFactory().numOf(100);
        ResistanceLevelIndicator.ResistanceWindowResult result = indicator.resistancePower(sorted, rangeThreshold, currentPrice);

        assertEquals(series.numFactory().zero(), result.getMaxResistancePower());
        assertEquals(series.numFactory().zero(), result.getTopPrice());
        assertEquals(series.numFactory().zero(), result.getBottomPrice());
        assertEquals(series.numFactory().zero(), result.getResistancePowerAbove());
        assertEquals(series.numFactory().zero(), result.getResistancePowerBelow());
    }

    @Test
    void resistancePower_shouldHandleMultipleWindowsAndChooseMaximum() {
        BarSeries series = seriesWithHighs(100);
        HighPriceIndicator high = new HighPriceIndicator(series);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series);

        Num rangeThreshold = series.numFactory().numOf(0.05); // 5%
        Num zoneThreshold = series.numFactory().numOf(0.20); // 20%

        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, close, strength, 32, rangeThreshold, zoneThreshold);

        // currentPrice = 100, zone = [80..120]
        // Создаём несколько возможных окон
        List<ResistanceLevelIndicator.PriceWithIndex> sorted = List.of(
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(110), 0, series.numFactory().numOf(3)),
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(105), 1, series.numFactory().numOf(10)), // Будет в лучшем окне
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(102), 2, series.numFactory().numOf(8)),  // Будет в лучшем окне
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(100), 3, series.numFactory().numOf(7)),  // Будет в лучшем окне
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(98), 4, series.numFactory().numOf(2)),
                new ResistanceLevelIndicator.PriceWithIndex(series.numFactory().numOf(90), 5, series.numFactory().numOf(1))
        );

        Num currentPrice = series.numFactory().numOf(100);

        // Окно с top=105: lower=105*0.95=99.75, currentPrice=100 попадает
        // В окно попадают: 105(str=10), 102(str=8), 100(str=7) => total=25
        // Это максимум, top=105, bottom=100
        // Above (>105): 110(str=3) => 3
        // Below (<100): 98(str=2), 90(str=1) => 3

        ResistanceLevelIndicator.ResistanceWindowResult result = indicator.resistancePower(sorted, rangeThreshold, currentPrice);

        assertEquals(series.numFactory().numOf(25), result.getMaxResistancePower());
        assertEquals(series.numFactory().numOf(105), result.getTopPrice());
        assertEquals(series.numFactory().numOf(100), result.getBottomPrice());
        assertEquals(series.numFactory().numOf(3), result.getResistancePowerAbove());
        assertEquals(series.numFactory().numOf(3), result.getResistancePowerBelow());
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

        // idx1: красная без тени, prev зелёная без тени => strength=2, high=120, close=120
        series.addBar(bar(t.plusSeconds(60), 120, 120, 100, 120));

        // idx2: красная без тени, prev красная без тени => strength=1, high=119, close=119
        series.addBar(bar(t.plusSeconds(120), 119, 119, 100, 119));

        // idx3: красная без тени, prev красная без тени -> strength=1, high=120, close=120
        series.addBar(bar(t.plusSeconds(180), 120, 120, 100, 120));

        HighPriceIndicator high = new HighPriceIndicator(series);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        CandleResistanceStrength strength = new CandleResistanceStrength(series, series.numFactory().numOf(0.03));

        // threshold 0.2% (0.002) как дефолт, barCount=32
        ResistanceLevelIndicator indicator = new ResistanceLevelIndicator(high, close, strength);

        // На idx3 currentPrice=close(3)=120. Анализируем только [0,1,2] (idx3 исключается).
        // Окно top=120(idx1): lower=120*(1-0.002)=119.76, в него попадают:
        //   - high 120(idx1, str=2)
        //   - high 119(idx2, str=1) - не попадает (119 < 119.76)
        // Итого: total=2.
        Num value = indicator.getValue(3);
        assertEquals(series.numFactory().numOf(2), value);
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
