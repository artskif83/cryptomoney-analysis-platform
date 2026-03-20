package artskif.trader.strategy.indicators.base;

import artskif.trader.strategy.indicators.util.IndicatorUtils;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LongLevelIndicator extends CachedIndicator<Num> {

    private final LowPriceIndicator lowPriceLowIndicator;
    private final ClosePriceIndicator closePriceIndicator;
    private final DoubleMAIndicator doubleMALowIndicator;
    private final DoubleMAIndicator doubleMAHighIndicator;
    private final int lowBarCount; // количество баров в котором считается поддержка нижнего таймфрейма
    private final Num longZonePercentagesLowThreshold; // окно в котором считается общая поддержка нижнего таймфрейма
    private final Num calculationZonePercentagesHighThreshold; // окно для расчета силы поддержки высшего таймфрейма, внутри которого должна находиться текущая цена
    private final Num stopLossPercentage; // процент отклонения стоп-лосса от цены поддержки

    public LongLevelIndicator(LowPriceIndicator lowPriceLowIndicator,
                              DoubleMAIndicator doubleMALowIndicator,
                              DoubleMAIndicator doubleMAHighIndicator,
                              ClosePriceIndicator closePriceIndicator,
                              int lowBarCount,
                              Num longZonePercentagesLowThreshold,
                              Num calculationZonePercentagesHighThreshold,
                              Num stopLossPercentage) {
        super(closePriceIndicator);
        this.lowPriceLowIndicator = lowPriceLowIndicator;
        this.closePriceIndicator = closePriceIndicator;
        this.doubleMALowIndicator = doubleMALowIndicator;
        this.doubleMAHighIndicator = doubleMAHighIndicator;
        this.lowBarCount = lowBarCount;
        this.longZonePercentagesLowThreshold = longZonePercentagesLowThreshold;
        this.calculationZonePercentagesHighThreshold = calculationZonePercentagesHighThreshold;
        this.stopLossPercentage = stopLossPercentage;
    }

    @Override
    protected Num calculate(int index) {
        int doubleMAlowerTfIndex = IndicatorUtils.mapToHigherTfIndex(closePriceIndicator.getBarSeries().getBar(index), doubleMALowIndicator.getBarSeries());
        int doubleMAhigherTfIndex = IndicatorUtils.mapToHigherTfIndex(closePriceIndicator.getBarSeries().getBar(index), doubleMAHighIndicator.getBarSeries());

        // Для лонга: DoubleMA нижнего ТФ должен быть >= 0 (восходящий тренд)
        if (doubleMALowIndicator.getValue(doubleMAlowerTfIndex).isLessThanOrEqual(DecimalNum.valueOf(0))) {
            return null;
        }
//        // Для лонга: DoubleMA высшего ТФ должен быть <= 0 (не нисходящий), т.е. >= 0
//        if (doubleMAHighIndicator.getValue(doubleMAhigherTfIndex).isLessThanOrEqual(DecimalNum.valueOf(0))) {
//            return null;
//        }

        List<PriceWithIndex> sortedLowPrices = sortByLowPrice(lowPriceLowIndicator, lowBarCount, index);

        Num longZoneBottomPrice = findLongZoneBottomPrice(sortedLowPrices, longZonePercentagesLowThreshold);

        if (longZoneBottomPrice == null) {
            return null;
        }

        Num closePriceIndicatorValue = closePriceIndicator.getValue(index);

        // Если текущая цена ниже зоны поддержки — возвращаем null
        if (closePriceIndicatorValue.isLessThan(longZoneBottomPrice)) {
            return null;
        }

        // Если расстояние между closePriceIndicatorValue и longZoneBottomPrice больше порога — возвращаем null
        Num hundred = DecimalNum.valueOf(100);
        Num distance = closePriceIndicatorValue.minus(longZoneBottomPrice).abs()
                .dividedBy(longZoneBottomPrice)
                .multipliedBy(hundred);
        if (distance.isGreaterThan(calculationZonePercentagesHighThreshold)) {
            return null;
        }

        return longZoneBottomPrice;
    }

    /**
     * Возвращает уровень стоп-лосса для лонга:
     * стоп-лосс = цена поддержки * (1 - stopLossPercentage / 100)
     */
    public Num getStopLos(int index) {
        Num longPrice = getValue(index);

        if (longPrice == null) {
            return null;
        }

        Num hundred = DecimalNum.valueOf(100);
        Num multiplier = DecimalNum.valueOf(1).minus(stopLossPercentage.dividedBy(hundred));
        return longPrice.multipliedBy(multiplier);
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
    List<PriceWithIndex> sortByLowPrice(LowPriceIndicator lowPriceIndicator,
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
     * Находит нижнюю цену зоны поддержки — первую (наименьшую) цену из пары,
     * в которой две цены отстоят друг от друга не более чем на {@code longZonePercentages} процентов.
     *
     * <p>Список {@code prices} должен быть отсортирован по возрастанию цены.
     * Для каждой пары {@code (prices[i], prices[j])} где {@code i < j} проверяется условие:
     * <pre>
     *   (price[j] - price[i]) / price[j] * 100 &lt;= longZonePercentages
     * </pre>
     * При выполнении условия возвращается скорректированная нижняя цена зоны:
     * {@code price[i] * (1 + longZonePercentages / 100)}.
     * Если подходящей пары не найдено — возвращается {@code null}.
     *
     * @param prices               список цен с индексами, отсортированный по возрастанию цены
     * @param longZonePercentages  максимально допустимый процент отклонения между двумя ценами
     * @return нижняя цена зоны поддержки, или {@code null} если зона не найдена
     */
    Num findLongZoneBottomPrice(List<PriceWithIndex> prices, Num longZonePercentages) {
        if (prices.size() < 2) {
            return null;
        }

        Num hundred = DecimalNum.valueOf(100);

        Num lowerPrice = prices.get(0).getPrice();
        Num upperPrice = prices.get(1).getPrice();

        // процент отклонения = (upperPrice - lowerPrice) / upperPrice * 100
        Num deviation = upperPrice.minus(lowerPrice)
                .dividedBy(upperPrice)
                .multipliedBy(hundred);

        if (deviation.isLessThanOrEqual(longZonePercentages)) {
            // возвращаем нижнюю цену плюс longZonePercentages процентов
            Num multiplier = DecimalNum.valueOf(100).plus(longZonePercentages).dividedBy(hundred);
            return lowerPrice.multipliedBy(multiplier);
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
