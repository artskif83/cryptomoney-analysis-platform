package artskif.trader.strategy.indicators.base;

import artskif.trader.strategy.indicators.util.IndicatorUtils;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.List;

public class ShortTrendIndicator extends CachedIndicator<Num> {

    private final HighPriceIndicator highPriceLowIndicator;
    private final ClosePriceIndicator closePriceIndicator;
    private final RSIIndicator rsiIndicator5m; // RSI индикатор для свечи 5 минут
    private final int lowBarCount; // количество баров в котором считается сопротивление нижнего таймфрейма
    private final Num shortZonePercentagesLowThreshold; // окно в котором считается общее сопротивление нижнего таймфрейма
    private final Num calculationZonePercentagesHighThreshold; // окно для расчета силы сопротивления высшего таймфрейма, внутри которого должна находиться текущая цена
    private final Num stopLossPercentage; // процент отклонения стоп-лосса от цены сопротивления

    public ShortTrendIndicator(HighPriceIndicator highPriceLowIndicator,
                               ClosePriceIndicator closePriceIndicator,
                               RSIIndicator rsiIndicator5m,
                               int lowBarCount,
                               Num shortZonePercentagesLowThreshold,
                               Num calculationZonePercentagesHighThreshold,
                               Num stopLossPercentage) {
        super(closePriceIndicator);
        this.highPriceLowIndicator = highPriceLowIndicator;
        this.closePriceIndicator = closePriceIndicator;
        this.rsiIndicator5m = rsiIndicator5m;
        this.lowBarCount = lowBarCount;
        this.shortZonePercentagesLowThreshold = shortZonePercentagesLowThreshold;
        this.calculationZonePercentagesHighThreshold = calculationZonePercentagesHighThreshold;
        this.stopLossPercentage = stopLossPercentage;
    }

    @Override
    protected Num calculate(int index) {
        int higherTfIndex = IndicatorUtils.mapToHigherTfIndex(closePriceIndicator.getBarSeries().getBar(index), rsiIndicator5m.getBarSeries());

        // Фильтр по RSI 5m: для шорта RSI должен быть выше 70
        Num rsiValue = rsiIndicator5m.getValue(higherTfIndex);
        if (rsiValue == null || rsiValue.isLessThan(DecimalNum.valueOf(70))) {
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

        // Прибавляем 0.02% к shortZoneTopPrice
        Num adjustedPrice = shortZoneTopPrice.minus(
                shortZoneTopPrice.multipliedBy(DecimalNum.valueOf(0.02)).dividedBy(DecimalNum.valueOf(100))
        );
        // Если скорректированная цена выше текущей — оставляем shortZoneTopPrice, иначе берём скорректированную
        if (adjustedPrice.isGreaterThan(closePriceIndicatorValue)) {
            shortZoneTopPrice = adjustedPrice;
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
     * @param prices               список цен с индексами, отсортированный по убыванию цены
     * @param shortZonePercentages максимально допустимый процент отклонения между двумя ценами
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
