package artskif.trader.strategy.indicators.base;

import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.List;

public class ShortLowLevelIndicator extends CachedIndicator<Num> {
    private final LowPriceIndicator lowPriceLowIndicator;
    private final ClosePriceIndicator closePriceIndicator;
    private final int lowBarCount; // количество баров в котором считается поддержка нижнего таймфрейма
    private final int highBarCount; // количество баров в котором считается поддержка высшего таймфрейма
    private final Num lowThresholdPercentages; // диапазон в котором считается что нашли уровень поддержки нижнего таймфрейма
    private final Num highThresholdPercentages; // диапазон в котором считается что нашли уровень поддержки высшего таймфрейма
    private final Num calculationZonePercentages; // окно в котором определяется точка входа
    private final Num stopLossPercentage; // процент отклонения стоп-лосса от цены поддержки

    public ShortLowLevelIndicator(
            LowPriceIndicator lowPriceLowIndicator,
            ClosePriceIndicator closePriceIndicator,
            int lowBarCount,
            int highBarCount,
            Num lowThresholdPercentages,
            Num highThresholdPercentages,
            Num calculationZonePercentages,
            Num stopLossPercentage) {
        super(closePriceIndicator);
        this.lowPriceLowIndicator = lowPriceLowIndicator;
        this.closePriceIndicator = closePriceIndicator;
        this.lowBarCount = lowBarCount;
        this.highBarCount = highBarCount;
        this.lowThresholdPercentages = lowThresholdPercentages;
        this.highThresholdPercentages = highThresholdPercentages;
        this.calculationZonePercentages = calculationZonePercentages;
        this.stopLossPercentage = stopLossPercentage;
    }

    @Override
    protected Num calculate(int index) {


        return null;
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
     * @param prices              список цен с индексами, отсортированный по возрастанию цены
     * @param longZonePercentages максимально допустимый процент отклонения между двумя ценами
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
