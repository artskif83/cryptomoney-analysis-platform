package artskif.trader.strategy.indicators.base;

import artskif.trader.strategy.indicators.util.IndicatorUtils;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ShortLevelIndicator extends CachedIndicator<Num> {

    private final HighPriceIndicator highPriceLowIndicator;
    private final ClosePriceIndicator closePriceIndicator;
    private final DoubleMAIndicator doubleMALowIndicator;
    private final DoubleMAIndicator doubleMAHighIndicator;
    private final int lowBarCount; // количество баров в котором считается сопротивление нижнего таймфрейма
    private final Num shortZonePercentagesLowThreshold; // окно в котором считается общее сопротивление нижнего таймфрейма
    private final Num calculationZonePercentagesHighThreshold; // окно для расчета силы сопротивления высшего таймфрейма, внутри которого должна находиться текущая цена
    private final Num stopLossPercentage; // процент отклонения стоп-лосса от цены сопротивления

    public ShortLevelIndicator(HighPriceIndicator highPriceLowIndicator,
                               DoubleMAIndicator doubleMALowIndicator,
                               DoubleMAIndicator doubleMAHighIndicator,
                               ClosePriceIndicator closePriceIndicator,
                               int lowBarCount,
                               Num shortZonePercentagesLowThreshold,
                               Num calculationZonePercentagesHighThreshold,
                               Num stopLossPercentage) {
        super(closePriceIndicator);
        this.highPriceLowIndicator = highPriceLowIndicator;
        this.closePriceIndicator = closePriceIndicator;
        this.doubleMALowIndicator = doubleMALowIndicator;
        this.doubleMAHighIndicator = doubleMAHighIndicator;
        this.lowBarCount = lowBarCount;
        this.shortZonePercentagesLowThreshold = shortZonePercentagesLowThreshold;
        this.calculationZonePercentagesHighThreshold = calculationZonePercentagesHighThreshold;
        this.stopLossPercentage = stopLossPercentage;
    }

    @Override
    protected Num calculate(int index) {
        int doubleMAlowerTfIndex = IndicatorUtils.mapToHigherTfIndex(closePriceIndicator.getBarSeries().getBar(index), doubleMALowIndicator.getBarSeries());
        int doubleMAhigherTfIndex = IndicatorUtils.mapToHigherTfIndex(closePriceIndicator.getBarSeries().getBar(index), doubleMAHighIndicator.getBarSeries());
        if (doubleMALowIndicator.getValue(doubleMAlowerTfIndex).isGreaterThan(DecimalNum.valueOf(0))){
            return null;
        }
        if (doubleMAHighIndicator.getValue(doubleMAhigherTfIndex).isGreaterThan(DecimalNum.valueOf(0))){
            return null;
        }

        List<PriceWithIndex> sortedLowPrices = sortByHighPrice(highPriceLowIndicator, lowBarCount, index);

        Num shortZoneTopPrice = findShortZoneTopPrice(sortedLowPrices, shortZonePercentagesLowThreshold);

        if (shortZoneTopPrice == null) {
            return null;
        }

        Num closePriceIndicatorValue = closePriceIndicator.getValue(index);

        // Если текущая цена выше зоны сопротивления — возвращаем null
        if (closePriceIndicatorValue.isGreaterThan(shortZoneTopPrice)) {
            return null;
        }

        // Если расстояние между shortZoneTopPrice и closePriceIndicatorValue больше порога — возвращаем null
        Num hundred = DecimalNum.valueOf(100);
        Num distance = shortZoneTopPrice.minus(closePriceIndicatorValue).abs()
                .dividedBy(shortZoneTopPrice)
                .multipliedBy(hundred);
        if (distance.isGreaterThan(calculationZonePercentagesHighThreshold)) {
            return null;
        }

        return shortZoneTopPrice;
    }


    public Num getStopLos(int index) {
        // Убеждаемся, что значение для данного индекса рассчитано
        Num shortPrice = getValue(index);

        if (shortPrice == null) {
            return null;
        }

        // Стоп лос = цена сопротивления * (1 + stopLossPercentage / 100)
        Num hundred = DecimalNum.valueOf(100);
        Num multiplier = DecimalNum.valueOf(1).plus(stopLossPercentage.dividedBy(hundred));
        return shortPrice.multipliedBy(multiplier);
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
        for (int i = startIndex; i <= currentIndex; i++) {
            Num highPrice = highPriceIndicator.getValue(i);
            pricesWithIndices.add(new PriceWithIndex(highPrice, i));
        }

        // Сортируем по убыванию цены (от максимальной к минимальной)
        pricesWithIndices.sort(Comparator.comparing(PriceWithIndex::getPrice).reversed());

        return pricesWithIndices;
    }



    /**
     * Находит верхнюю цену зоны сопротивления — первую (наибольшую) цену из пары,
     * в которой две цены отстоят друг от друга не более чем на {@code shortZonePercentages} процентов.
     *
     * <p>Список {@code prices} должен быть отсортирован по убыванию цены.
     * Для каждой пары {@code (prices[i], prices[j])} где {@code i < j} проверяется условие:
     * <pre>
     *   (price[i] - price[j]) / price[i] * 100 &lt;= shortZonePercentages
     * </pre>
     * При выполнении условия возвращается {@code price[i]} — верхняя (большая) цена зоны.
     * Если подходящей пары не найдено — возвращается {@code null}.
     *
     * @param prices                          список цен с индексами, отсортированный по убыванию цены
     * @param shortZonePercentages       максимально допустимый процент отклонения между двумя ценами
     * @return верхняя цена зоны сопротивления, или {@code null} если зона не найдена
     */
    Num findShortZoneTopPrice(List<PriceWithIndex> prices, Num shortZonePercentages) {
        if (prices.size() < 2) {
            return null;
        }

        Num hundred = DecimalNum.valueOf(100);

        Num upperPrice = prices.get(0).getPrice();
        Num lowerPrice = prices.get(1).getPrice();

        // процент отклонения = (upperPrice - lowerPrice) / upperPrice * 100
        Num deviation = upperPrice.minus(lowerPrice)
                .dividedBy(upperPrice)
                .multipliedBy(hundred);

        if (deviation.isLessThanOrEqual(shortZonePercentages)) {
            // возвращаем верхнюю цену минус shortZonePercentages процентов
            Num multiplier = DecimalNum.valueOf(100).minus(shortZonePercentages).dividedBy(hundred);
            return upperPrice.multipliedBy(multiplier);
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
