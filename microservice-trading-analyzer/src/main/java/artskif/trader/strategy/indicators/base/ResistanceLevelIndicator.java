package artskif.trader.strategy.indicators.base;

import artskif.trader.strategy.indicators.util.IndicatorUtils;
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

    private final HighPriceIndicator highPriceHighIndicator;
    private final HighPriceIndicator highPriceLowIndicator;
    private final ClosePriceIndicator closePriceIndicator;
    private final int highBarCount; // количество баров в котором считается сопротивление высшего таймфрейма
    private final int lowBarCount; // количество баров в котором считается сопротивление нижнего таймфрейма
    private final Num resistanceZonePercentagesHighThreshold; // окно в котором считается общее сопротивление высшего таймфрейма
    private final Num resistanceZonePercentagesLowThreshold; // окно в котором считается общее сопротивление нижнего таймфрейма
    private final Num calculationZonePercentagesHighThreshold; // окно для расчета силы сопротивления высшего таймфрейма, внутри которого должна находиться текущая цена

    public ResistanceLevelIndicator(HighPriceIndicator highPriceHighIndicator,
                                    HighPriceIndicator highPriceLowIndicator,
                                    ClosePriceIndicator closePriceIndicator,
                                    int highBarCount,
                                    int lowBarCount,
                                    Num resistanceZonePercentagesHighThreshold,
                                    Num resistanceZonePercentagesLowThreshold,
                                    Num calculationZonePercentagesHighThreshold) {
        super(closePriceIndicator);
        this.highPriceHighIndicator = highPriceHighIndicator;
        this.highPriceLowIndicator = highPriceLowIndicator;
        this.closePriceIndicator = closePriceIndicator;
        this.highBarCount = highBarCount;
        this.lowBarCount = lowBarCount;
        this.resistanceZonePercentagesHighThreshold = resistanceZonePercentagesHighThreshold;
        this.resistanceZonePercentagesLowThreshold = resistanceZonePercentagesLowThreshold;
        this.calculationZonePercentagesHighThreshold = calculationZonePercentagesHighThreshold;
    }

    @Override
    protected Num calculate(int index) {
        int higherTfIndex = IndicatorUtils.mapToHigherTfIndex(closePriceIndicator.getBarSeries().getBar(index), highPriceHighIndicator.getBarSeries());
        int lowerTfIndex = IndicatorUtils.mapToHigherTfIndex(closePriceIndicator.getBarSeries().getBar(index), highPriceLowIndicator.getBarSeries());


        List<PriceWithIndex> sortedHighPrices = sortByHighPrice(highPriceHighIndicator, highBarCount, higherTfIndex);
        List<PriceWithIndex> sortedLowPrices = sortByHighPrice(highPriceLowIndicator, lowBarCount, lowerTfIndex);

        Num resistanceZoneTopPrice = findResistanceZoneTopPrice(sortedHighPrices, resistanceZonePercentagesHighThreshold);

        return  resistanceZoneTopPrice;
    }


    public Num getStopLos(int index) {
        // Убеждаемся, что значение для данного индекса рассчитано
        Num resistancePrice = getValue(index);

        if (resistancePrice == null) {
            return null;
        }

        // Стоп лос = цена сопротивления + 0.1%
        Num multiplier = DecimalNum.valueOf("1.001");
        return resistancePrice.multipliedBy(multiplier);
    }

    /**
     * Сортирует элементы по значениям индикатора highPriceIndicator, сохраняя исходные индексы
     * @param highPriceIndicator индикатор максимальной цены
     * @param currentIndex текущий индекс
     * @return отсортированный список элементов с сохраненными индексами
     */
    List<PriceWithIndex> sortByHighPrice(HighPriceIndicator highPriceIndicator,
                                                   int highBarCount,
                                                   int currentIndex) {
        List<PriceWithIndex> pricesWithIndices = new ArrayList<>();

        // Определяем начальный индекс для анализа
        int startIndex = Math.max(0, currentIndex - highBarCount + 1);

        // Собираем значения цен с их индексами
        for (int i = startIndex; i < currentIndex; i++) {
            Num highPrice = highPriceIndicator.getValue(i);
            pricesWithIndices.add(new PriceWithIndex(highPrice, i));
        }

        // Сортируем по убыванию цены (от максимальной к минимальной)
        pricesWithIndices.sort(Comparator.comparing(PriceWithIndex::getPrice).reversed());

        return pricesWithIndices;
    }



    /**
     * Находит верхнюю цену зоны сопротивления — первую (наибольшую) цену из пары,
     * в которой две цены отстоят друг от друга не более чем на {@code resistanceZonePercentages} процентов.
     *
     * <p>Список {@code prices} должен быть отсортирован по убыванию цены.
     * Для каждой пары {@code (prices[i], prices[j])} где {@code i < j} проверяется условие:
     * <pre>
     *   (price[i] - price[j]) / price[i] * 100 &lt;= resistanceZonePercentages
     * </pre>
     * При выполнении условия возвращается {@code price[i]} — верхняя (большая) цена зоны.
     * Если подходящей пары не найдено — возвращается {@code null}.
     *
     * @param prices                          список цен с индексами, отсортированный по убыванию цены
     * @param resistanceZonePercentages       максимально допустимый процент отклонения между двумя ценами
     * @return верхняя цена зоны сопротивления, или {@code null} если зона не найдена
     */
    Num findResistanceZoneTopPrice(List<PriceWithIndex> prices, Num resistanceZonePercentages) {
        Num hundred = DecimalNum.valueOf(100);

        for (int i = 0; i < prices.size(); i++) {
            Num upperPrice = prices.get(i).getPrice();

            for (int j = i + 1; j < prices.size(); j++) {
                Num lowerPrice = prices.get(j).getPrice();

                // процент отклонения = (upperPrice - lowerPrice) / upperPrice * 100
                Num deviation = upperPrice.minus(lowerPrice)
                        .dividedBy(upperPrice)
                        .multipliedBy(hundred);

                if (deviation.isLessThanOrEqual(resistanceZonePercentages)) {
                    return upperPrice;
                }
            }
        }

        return null;
    }

    /**
     * Вспомогательный класс для хранения цены и её исходного индекса
     */
    static class PriceWithIndex {
        private final Num price;
        private final int originalIndex;

        PriceWithIndex(Num price, int originalIndex) {
            this.price = price;
            this.originalIndex = originalIndex;
        }

        public Num getPrice() {
            return price;
        }

        public int getOriginalIndex() {
            return originalIndex;
        }

        @Override
        public String toString() {
            return "PriceWithIndex{" +
                    "price=" + price +
                    ", originalIndex=" + originalIndex +
                    '}';
        }
    }

    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }
}
