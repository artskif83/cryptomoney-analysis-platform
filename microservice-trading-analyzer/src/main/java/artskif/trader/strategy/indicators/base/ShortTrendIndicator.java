package artskif.trader.strategy.indicators.base;

import artskif.trader.strategy.indicators.util.IndicatorUtils;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.List;

public class ShortTrendIndicator extends CachedIndicator<Num> {

    private final HighPriceIndicator highPriceLowIndicator;
    private final ClosePriceIndicator closePriceIndicator;
    private final DoubleMAIndicator doubleMALowIndicator;
    private final DoubleMAIndicator doubleMAHighIndicator;
    private final LongHighLevelIndicator longHighLevelIndicator; // часовой уровень сопротивления старшего таймфрейма
    private final ShortHighLevelIndicator shortHighLevelIndicator; // часовой уровень поддержки старшего таймфрейма
    private final int lowBarCount; // количество баров в котором считается сопротивление нижнего таймфрейма
    private final Num shortZonePercentagesLowThreshold; // окно в котором считается общее сопротивление нижнего таймфрейма
    private final Num calculationZonePercentagesHighThreshold; // окно для расчета силы сопротивления высшего таймфрейма, внутри которого должна находиться текущая цена
    private final Num stopLossPercentage; // процент отклонения стоп-лосса от цены сопротивления

    public ShortTrendIndicator(HighPriceIndicator highPriceLowIndicator,
                               DoubleMAIndicator doubleMALowIndicator,
                               DoubleMAIndicator doubleMAHighIndicator,
                               ADXAngleIndicator adxAngleIndicator,
                               LongHighLevelIndicator longHighLevelIndicator,
                               ShortHighLevelIndicator shortHighLevelIndicator,
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
        this.longHighLevelIndicator = longHighLevelIndicator;
        this.shortHighLevelIndicator = shortHighLevelIndicator;
        this.lowBarCount = lowBarCount;
        this.shortZonePercentagesLowThreshold = shortZonePercentagesLowThreshold;
        this.calculationZonePercentagesHighThreshold = calculationZonePercentagesHighThreshold;
        this.stopLossPercentage = stopLossPercentage;
    }

    @Override
    protected Num calculate(int index) {
        int shortLowLevelIndex = IndicatorUtils.mapToHigherTfIndex(closePriceIndicator.getBarSeries().getBar(index), doubleMALowIndicator.getBarSeries());
        int shortHighLevelIndex = IndicatorUtils.mapToHigherTfIndex(closePriceIndicator.getBarSeries().getBar(index), doubleMAHighIndicator.getBarSeries());

        Num longLevelTop = longHighLevelIndicator.getTopBorder(shortHighLevelIndex);
        Num longLevelBottom = longHighLevelIndicator.getBottomBorder(shortHighLevelIndex);

        Num shortLevelTop = shortHighLevelIndicator.getTopBorder(shortHighLevelIndex);
        Num shortLevelBottom = shortHighLevelIndicator.getBottomBorder(shortHighLevelIndex);
        Num currentPrice = closePriceIndicator.getValue(index);

        boolean isCalculate = false;

        if (doubleMAHighIndicator.getValue(shortHighLevelIndex).isGreaterThanOrEqual(DecimalNum.valueOf(0))){
            return null;
        }

        // Пробили уровень ищем ретест
        if (shortLevelTop != null && shortLevelBottom != null
                && currentPrice.isGreaterThan(shortLevelBottom)
                && currentPrice.isLessThan(shortLevelTop)
                && doubleMALowIndicator.getValue(shortLowLevelIndex).isGreaterThan(DecimalNum.valueOf(0))) {
            isCalculate = true;
        }

        // Подошли к уровню ищем сопротивление
        if (longLevelTop != null && longLevelBottom != null
                && currentPrice.isGreaterThan(longLevelBottom)
                && currentPrice.isLessThan(longLevelTop)
                && doubleMALowIndicator.getValue(shortLowLevelIndex).isGreaterThan(DecimalNum.valueOf(0))) {
            isCalculate = true;
        }

        if (!isCalculate) {
            return null;
        }

        List<PriceWithIndex> sortedLowPrices = IndicatorUtils.sortByHighPrice(highPriceLowIndicator, lowBarCount, index);

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
            // возвращаем верхнюю цену
            return upperPrice;
        }

        return null;
    }


    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }
}
