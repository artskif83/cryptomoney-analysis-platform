package artskif.trader.strategy.indicators.base;

import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResistanceLevelIndicator  extends CachedIndicator<Num> {

    private final HighPriceIndicator highPriceIndicator;
    private final CandleResistanceStrength candleResistanceStrength;
    private final ClosePriceIndicator closePriceIndicator;
    private final int barCount;
    private final Num resistanceRangePercentagesThreshold; // Узкое окно в котором находится цена и считает итоговое сопротивление
    private final Num resistanceZonePercentagesThreshold; // широкое окно в котором считается общее сопротивление

    // Кэш для хранения результатов расчетов по каждому индексу
    private final Map<Integer, ResistanceWindowResult> resultsCache = new HashMap<>();

    public ResistanceLevelIndicator(HighPriceIndicator highPriceIndicator,
                                    ClosePriceIndicator closePriceIndicator,
                                    CandleResistanceStrength candleResistanceStrength) {
        this(highPriceIndicator, closePriceIndicator, candleResistanceStrength, 12, DecimalNum.valueOf(0.0005), DecimalNum.valueOf(0.005));

    }

    public ResistanceLevelIndicator(HighPriceIndicator highPriceIndicator,
                                    ClosePriceIndicator closePriceIndicator,
                                    CandleResistanceStrength candleResistanceStrength,
                                    int barCount,
                                    Num resistanceRangePercentagesThreshold,
                                    Num resistanceZonePercentagesThreshold) {
        super(highPriceIndicator);
        this.highPriceIndicator = highPriceIndicator;
        this.candleResistanceStrength = candleResistanceStrength;
        this.closePriceIndicator = closePriceIndicator;
        this.barCount = barCount;
        this.resistanceRangePercentagesThreshold = resistanceRangePercentagesThreshold;
        this.resistanceZonePercentagesThreshold = resistanceZonePercentagesThreshold;
    }

    @Override
    protected Num calculate(int index) {
        List<PriceWithIndex> sortedPrices = sortByHighPrice(highPriceIndicator, candleResistanceStrength, index);

        ResistanceWindowResult result = resistancePower(sortedPrices,
                resistanceRangePercentagesThreshold, highPriceIndicator.getValue(index));

        // Сохраняем результат в кэш для дальнейшего использования
        resultsCache.put(index, result);

        return  result.getMaxResistancePower().minus(result.getResistancePowerAbove()).max(DecimalNum.valueOf(0));
    }

    /**
     * Возвращает силу сопротивления выше окна для указанного индекса.
     * Перед вызовом этого метода необходимо вызвать getValue(index),
     * чтобы убедиться, что результат для данного индекса был рассчитан и закэширован.
     *
     * @param index индекс бара
     * @return сила сопротивления выше окна или ноль, если результат еще не рассчитан
     */
    public Num getResistancePowerAbove(int index) {
        // Убеждаемся, что значение для данного индекса рассчитано
        getValue(index);

        ResistanceWindowResult result = resultsCache.get(index);
        if (result != null) {
            return result.getResistancePowerAbove();
        }
        return getBarSeries().numFactory().zero();
    }

    /**
     * Вычисляет максимальную силу сопротивления для окон, содержащих текущую цену currentPrice.
     * Алгоритм:
     * 1. Фильтрует цены, находящиеся в пределах resistanceZonePercentagesThreshold от currentPrice
     * 2. Для каждого элемента из отфильтрованного списка как верхней границы окна
     * 3. Вычисляет окно размером resistanceRangePercentagesThreshold
     * 4. Если currentPrice попадает в это окно, суммирует силу сопротивления всех элементов в окне
     * 5. Находит окно с максимальной силой сопротивления
     * 6. Возвращает результат с верхней/нижней ценой окна и суммой сопротивлений выше и ниже окна
     *
     * @param sortedPrices отсортированный список цен (от максимальной к минимальной) с силой сопротивления
     * @param resistanceRangePercentagesThreshold процентный порог для определения размера окна
     * @param currentPrice текущая цена, которая должна попасть в окно
     * @return результат с информацией о наилучшем окне сопротивления
     */
    ResistanceWindowResult resistancePower(List<PriceWithIndex> sortedPrices, Num resistanceRangePercentagesThreshold, Num currentPrice) {
        if (sortedPrices.isEmpty()) {
            Num zero = getBarSeries().numFactory().zero();
            return new ResistanceWindowResult(zero, zero, zero, zero, zero);
        }

        // Шаг 1: Фильтруем цены в пределах resistanceZonePercentagesThreshold от currentPrice
        Num zoneLowerBound = currentPrice.multipliedBy(
            getBarSeries().numFactory().one().minus(resistanceZonePercentagesThreshold)
        );
        Num zoneUpperBound = currentPrice.multipliedBy(
            getBarSeries().numFactory().one().plus(resistanceZonePercentagesThreshold)
        );

        List<PriceWithIndex> filteredPrices = new ArrayList<>();
        for (PriceWithIndex priceItem : sortedPrices) {
            if (priceItem.getPrice().isGreaterThanOrEqual(zoneLowerBound) &&
                priceItem.getPrice().isLessThanOrEqual(zoneUpperBound)) {
                filteredPrices.add(priceItem);
            }
        }

        if (filteredPrices.isEmpty()) {
            Num zero = getBarSeries().numFactory().zero();
            return new ResistanceWindowResult(zero, zero, zero, zero, zero);
        }

        Num maxResistancePower = getBarSeries().numFactory().zero();
        Num bestTopPrice = filteredPrices.getFirst().getPrice(); // Изначально верхняя цена - это максимальная цена из отфильтрованных
        Num bestBottomPrice = filteredPrices.getLast().getPrice(); // Изначально нижняя цена - это минимальная цена из отфильтрованных

        // Шаг 2-5: Находим окно с максимальной силой сопротивления
        for (int i = 0; i < filteredPrices.size(); i++) {
            PriceWithIndex topLevel = filteredPrices.get(i);
            Num topPrice = topLevel.getPrice();

            // Вычисляем нижнюю границу окна: topPrice * (1 - resistanceRangePercentagesThreshold)
            Num lowerBound = topPrice.multipliedBy(
                getBarSeries().numFactory().one().minus(resistanceRangePercentagesThreshold)
            );

            // Проверяем, попадает ли currentPrice в окно [lowerBound, topPrice]
            if (currentPrice.isGreaterThanOrEqual(lowerBound) && currentPrice.isLessThanOrEqual(topPrice)) {
                // currentPrice попадает в окно - суммируем силу сопротивления всех элементов в этом окне
                Num totalResistance = getBarSeries().numFactory().zero();
                Num windowBottomPrice = topPrice;

                // Проходим по всем элементам и суммируем те, что попадают в окно
                for (int j = i; j < filteredPrices.size(); j++) {
                    PriceWithIndex priceItem = filteredPrices.get(j);
                    Num price = priceItem.getPrice();

                    // Проверяем, попадает ли цена в окно [lowerBound, topPrice]
                    if (price.isGreaterThanOrEqual(lowerBound) && price.isLessThanOrEqual(topPrice)) {
                        totalResistance = totalResistance.plus(priceItem.getResistanceStrength());
                        // Обновляем нижнюю цену окна
                        if (price.isLessThan(windowBottomPrice)) {
                            windowBottomPrice = price;
                        }
                    } else if (price.isLessThan(lowerBound)) {
                        // Цена вышла за нижнюю границу окна, дальше проверять нет смысла
                        break;
                    }
                }

                // Обновляем максимальную силу сопротивления, если нашли большее значение
                if (totalResistance.isGreaterThan(maxResistancePower)) {
                    maxResistancePower = totalResistance;
                    bestTopPrice = topPrice;
                    bestBottomPrice = windowBottomPrice;
                }
            }
        }

        // Шаг 6: Вычисляем суммы сопротивлений выше и ниже наилучшего окна
        Num resistancePowerAbove = getBarSeries().numFactory().zero();
        Num resistancePowerBelow = getBarSeries().numFactory().zero();

        for (PriceWithIndex priceItem : filteredPrices) {
            Num price = priceItem.getPrice();
            if (price.isGreaterThan(bestTopPrice)) {
                resistancePowerAbove = resistancePowerAbove.plus(priceItem.getResistanceStrength());
            } else if (price.isLessThan(bestBottomPrice)) {
                resistancePowerBelow = resistancePowerBelow.plus(priceItem.getResistanceStrength());
            }
        }

        return new ResistanceWindowResult(maxResistancePower, bestTopPrice, bestBottomPrice,
                                         resistancePowerAbove, resistancePowerBelow);
    }

    /**
     * Сортирует элементы по значениям индикатора highPriceIndicator, сохраняя исходные индексы
     * и добавляя силу сопротивления из индикатора candleResistanceStrength
     * @param highPriceIndicator индикатор максимальной цены
     * @param candleResistanceStrength индикатор силы сопротивления свечи
     * @param currentIndex текущий индекс
     * @return отсортированный список элементов с сохраненными индексами и силой сопротивления
     */
    List<PriceWithIndex> sortByHighPrice(HighPriceIndicator highPriceIndicator,
                                                   CandleResistanceStrength candleResistanceStrength,
                                                   int currentIndex) {
        List<PriceWithIndex> pricesWithIndices = new ArrayList<>();

        // Определяем начальный индекс для анализа
        int startIndex = Math.max(0, currentIndex - barCount + 1);

        // Собираем значения цен с их индексами и силой сопротивления
        for (int i = startIndex; i < currentIndex; i++) {
            Num highPrice = highPriceIndicator.getValue(i);
            Num resistanceStrength = candleResistanceStrength.getValue(i);
            pricesWithIndices.add(new PriceWithIndex(highPrice, i, resistanceStrength));
        }

        // Сортируем по убыванию цены (от максимальной к минимальной)
        pricesWithIndices.sort(Comparator.comparing(PriceWithIndex::getPrice).reversed());

        return pricesWithIndices;
    }

    /**
     * Вспомогательный класс для хранения цены, её исходного индекса и силы сопротивления
     */
    static class PriceWithIndex {
        private final Num price;
        private final int originalIndex;
        private final Num resistanceStrength;

        PriceWithIndex(Num price, int originalIndex, Num resistanceStrength) {
            this.price = price;
            this.originalIndex = originalIndex;
            this.resistanceStrength = resistanceStrength;
        }

        public Num getPrice() {
            return price;
        }

        public int getOriginalIndex() {
            return originalIndex;
        }

        public Num getResistanceStrength() {
            return resistanceStrength;
        }

        @Override
        public String toString() {
            return "PriceWithIndex{" +
                   "price=" + price +
                   ", originalIndex=" + originalIndex +
                   ", resistanceStrength=" + resistanceStrength +
                   '}';
        }
    }

    /**
     * Класс для хранения результатов анализа окна сопротивления
     */
    static class ResistanceWindowResult {
        private final Num maxResistancePower;
        private final Num topPrice;
        private final Num bottomPrice;
        private final Num resistancePowerAbove;
        private final Num resistancePowerBelow;

        ResistanceWindowResult(Num maxResistancePower, Num topPrice, Num bottomPrice,
                               Num resistancePowerAbove, Num resistancePowerBelow) {
            this.maxResistancePower = maxResistancePower;
            this.topPrice = topPrice;
            this.bottomPrice = bottomPrice;
            this.resistancePowerAbove = resistancePowerAbove;
            this.resistancePowerBelow = resistancePowerBelow;
        }

        public Num getMaxResistancePower() {
            return maxResistancePower;
        }

        public Num getTopPrice() {
            return topPrice;
        }

        public Num getBottomPrice() {
            return bottomPrice;
        }

        public Num getResistancePowerAbove() {
            return resistancePowerAbove;
        }

        public Num getResistancePowerBelow() {
            return resistancePowerBelow;
        }
    }

    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }
}
