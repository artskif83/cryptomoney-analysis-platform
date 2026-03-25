package artskif.trader.strategy.indicators.base;

import artskif.trader.strategy.indicators.util.IndicatorUtils;
import org.ta4j.core.Bar;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.List;

public class LongTrendIndicator extends CachedIndicator<Num> {

    private final LowPriceIndicator lowPriceLowIndicator;
    private final ClosePriceIndicator closePriceIndicator;
    private final DoubleMAIndicator doubleMALowIndicator;
    private final DoubleMAIndicator doubleMAHighIndicator;
    private final ADXAngleIndicator adxAngleIndicator;
    private final LongHighLevelIndicator longHighLevelIndicator; // часовой уровень поддержки
    private final int lowBarCount; // количество баров в котором считается поддержка нижнего таймфрейма
    private final Num longZonePercentagesLowThreshold; // окно в котором считается общая поддержка нижнего таймфрейма
    private final Num calculationZonePercentagesHighThreshold; // окно для расчета силы поддержки высшего таймфрейма, внутри которого должна находиться текущая цена
    private final Num stopLossPercentage; // процент отклонения стоп-лосса от цены поддержки

    public LongTrendIndicator(LowPriceIndicator lowPriceLowIndicator,
                              DoubleMAIndicator doubleMALowIndicator,
                              DoubleMAIndicator doubleMAHighIndicator,
                              ADXAngleIndicator adxAngleIndicator,
                              LongHighLevelIndicator longHighLevelIndicator,
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
        this.adxAngleIndicator = adxAngleIndicator;
        this.longHighLevelIndicator = longHighLevelIndicator;
        this.lowBarCount = lowBarCount;
        this.longZonePercentagesLowThreshold = longZonePercentagesLowThreshold;
        this.calculationZonePercentagesHighThreshold = calculationZonePercentagesHighThreshold;
        this.stopLossPercentage = stopLossPercentage;
    }

    @Override
    protected Num calculate(int index) {
        int longLowLevelIndex = IndicatorUtils.mapToHigherTfIndex(closePriceIndicator.getBarSeries().getBar(index), doubleMALowIndicator.getBarSeries());
        int longHighLevelIndex = IndicatorUtils.mapToHigherTfIndex(closePriceIndicator.getBarSeries().getBar(index), doubleMAHighIndicator.getBarSeries());
        Bar highBar = doubleMAHighIndicator.getBarSeries().getBar(longHighLevelIndex);

        boolean isCalculating = false;

        Num highLevelTop = longHighLevelIndicator.getTopBorder(longHighLevelIndex);
        Num highLevelBottom = longHighLevelIndicator.getBottomBorder(longHighLevelIndex);
        Num currentPrice = closePriceIndicator.getValue(index);
        if (highLevelTop != null && highLevelBottom != null && currentPrice.isGreaterThanOrEqual(highLevelBottom) && currentPrice.isLessThanOrEqual(highLevelTop)) {
            isCalculating = true;
        }
//
//        if (!isCalculating && adxAngleIndicator.isRising(longHighLevelIndex) &&
//                doubleMALowIndicator.getValue(longLowLevelIndex).isGreaterThan(DecimalNum.valueOf(0)) &&
//                doubleMAHighIndicator.getValue(longHighLevelIndex).isGreaterThan(DecimalNum.valueOf(0))) {
//            isCalculating = true;
//        }

        if (!isCalculating && highBar.isBullish()
                && doubleMALowIndicator.getValue(longLowLevelIndex).isGreaterThan(DecimalNum.valueOf(0))) {
            isCalculating = true;
        }

        if (!isCalculating) {
            return null;
        }

        List<PriceWithIndex> sortedLowPrices = IndicatorUtils.sortByLowPrice(lowPriceLowIndicator, lowBarCount, index);

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


    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }
}
