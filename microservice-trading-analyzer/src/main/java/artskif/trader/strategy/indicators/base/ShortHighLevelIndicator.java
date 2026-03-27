package artskif.trader.strategy.indicators.base;

import artskif.trader.strategy.indicators.util.IndicatorUtils;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.List;

public class ShortHighLevelIndicator extends CachedIndicator<Num> {

    private final LowPriceIndicator lowPriceIndicator;
    private final int highBarCount; // количество баров в котором считается поддержка высшего таймфрейма
    private final Num highThresholdPercentages; // диапазон в котором считается что нашли уровень поддержки высшего таймфрейма
    private final Num calculationRadiusPercentages; // окно в котором определяется точка входа

    public ShortHighLevelIndicator(
            LowPriceIndicator lowPriceIndicator,
            ClosePriceIndicator closePriceIndicator,
            int highBarCount,
            Num highThresholdPercentages,
            Num calculationRadiusPercentages
    ) {
        super(closePriceIndicator);
        this.lowPriceIndicator = lowPriceIndicator;
        this.highBarCount = highBarCount;
        this.highThresholdPercentages = highThresholdPercentages;
        this.calculationRadiusPercentages = calculationRadiusPercentages;
    }

    @Override
    protected Num calculate(int index) {

        List<PriceWithIndex> sortedPrices = IndicatorUtils.sortByLowPrice(lowPriceIndicator, highBarCount, index);

        return findLowestPriceAmongThree(sortedPrices);
    }


    public Num getTopBorder(int index) {
        Num price = getValue(index);
        if (price == null) {
            return null;
        }
        // price * (1 + calculationRadiusPercentages / 100)
        Num hundred = DecimalNum.valueOf(100);
        Num multiplier = hundred.plus(calculationRadiusPercentages).dividedBy(hundred);
        return price.multipliedBy(multiplier);
    }

    public Num getBottomBorder(int index) {
        Num price = getValue(index);
        if (price == null) {
            return null;
        }
        // price * (1 - calculationRadiusPercentages / 100)
        Num hundred = DecimalNum.valueOf(100);
        Num multiplier = hundred.minus(calculationRadiusPercentages).dividedBy(hundred);
        return price.multipliedBy(multiplier);
    }

    /**
     * Находит наименьшую цену среди трёх цен, которые отстоят друг от друга
     * не более чем на {@code highThresholdPercentages} процентов.
     *
     * <p>Список {@code prices} должен быть отсортирован по возрастанию цены.
     * Для каждой тройки {@code (prices[i], prices[j], prices[k])} где {@code i < j < k}
     * проверяется условие:
     * <pre>
     *   (prices[k] - prices[i]) / prices[k] * 100 &lt;= highThresholdPercentages
     * </pre>
     * При выполнении условия возвращается наименьшая цена тройки {@code prices[i].getPrice()}.
     * Если подходящей тройки не найдено — возвращается {@code null}.
     *
     * @param prices список цен с индексами, отсортированный по возрастанию цены
     * @return наименьшая цена зоны из трёх, или {@code null} если зона не найдена
     */
    Num findLowestPriceAmongThree(List<PriceWithIndex> prices) {
        if (prices.size() < 3) {
            return null;
        }

        Num hundred = DecimalNum.valueOf(100);

        for (int i = 0; i <= prices.size() - 2; i++) {
            Num lowerPrice = prices.get(i).getPrice();
            Num upperPrice = prices.get(i + 1).getPrice();

            // процент отклонения = (upperPrice - lowerPrice) / upperPrice * 100
            Num deviation = upperPrice.minus(lowerPrice)
                    .dividedBy(upperPrice)
                    .multipliedBy(hundred);

            if (deviation.isLessThanOrEqual(highThresholdPercentages)) {
                return lowerPrice;
            }
        }

        return null;
    }

    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }
}
