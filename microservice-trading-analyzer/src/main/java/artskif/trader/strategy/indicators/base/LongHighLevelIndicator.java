package artskif.trader.strategy.indicators.base;

import artskif.trader.strategy.indicators.util.IndicatorUtils;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.List;

/**
 * Индикатор уровня сопротивления старшего таймфрейма для лонговой позиции.
 *
 * <p>Зеркальный аналог {@link ShortHighLevelIndicator}: вместо поиска уровня поддержки
 * по минимальным ценам ищет уровень сопротивления по максимальным ценам (High).
 * Список баров сортируется по убыванию цены, и среди двух соседних High-цен,
 * отклонение которых не превышает {@code highThresholdPercentages}, возвращается
 * наибольшая (верхняя) цена кластера.
 */
public class LongHighLevelIndicator extends CachedIndicator<Num> {

    private final HighPriceIndicator highPriceIndicator;
    private final int highBarCount; // количество баров в котором считается сопротивление высшего таймфрейма
    private final Num highThresholdPercentages; // диапазон в котором считается что нашли уровень сопротивления высшего таймфрейма
    private final Num calculationRadiusPercentages; // окно в котором определяется точка входа

    public LongHighLevelIndicator(
            HighPriceIndicator highPriceIndicator,
            ClosePriceIndicator closePriceIndicator,
            int highBarCount,
            Num highThresholdPercentages,
            Num calculationRadiusPercentages
    ) {
        super(closePriceIndicator);
        this.highPriceIndicator = highPriceIndicator;
        this.highBarCount = highBarCount;
        this.highThresholdPercentages = highThresholdPercentages;
        this.calculationRadiusPercentages = calculationRadiusPercentages;
    }

    @Override
    protected Num calculate(int index) {
        List<PriceWithIndex> sortedPrices = IndicatorUtils.sortByHighPrice(highPriceIndicator, highBarCount, index);
        return findHighestPriceAmongThree(sortedPrices);
    }

    /**
     * Возвращает верхнюю границу зоны сопротивления:
     * {@code price * (1 + calculationRadiusPercentages / 100)}.
     */
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

    /**
     * Возвращает нижнюю границу зоны сопротивления — саму цену уровня.
     */
    public Num getBottomBorder(int index) {
        Num price = getValue(index);
        if (price == null) {
            return null;
        }
        return price;
    }

    /**
     * Находит наибольшую цену среди двух соседних цен, которые отстоят друг от друга
     * не более чем на {@code highThresholdPercentages} процентов.
     *
     * <p>Список {@code prices} должен быть отсортирован по убыванию цены.
     * Для каждой пары {@code (prices[i], prices[i+1])} проверяется условие:
     * <pre>
     *   (prices[i] - prices[i+1]) / prices[i] * 100 &lt;= highThresholdPercentages
     * </pre>
     * При выполнении условия возвращается наибольшая цена пары {@code prices[i].getPrice()}.
     * Если подходящей пары не найдено — возвращается {@code null}.
     *
     * @param prices список цен с индексами, отсортированный по убыванию цены
     * @return наибольшая цена зоны или {@code null} если зона не найдена
     */
    Num findHighestPriceAmongThree(List<PriceWithIndex> prices) {
        if (prices.size() < 3) {
            return null;
        }

        Num hundred = DecimalNum.valueOf(100);

        for (int i = 0; i <= prices.size() - 2; i++) {
            Num upperPrice = prices.get(i).getPrice();
            Num lowerPrice = prices.get(i + 1).getPrice();

            // процент отклонения = (upperPrice - lowerPrice) / upperPrice * 100
            Num deviation = upperPrice.minus(lowerPrice)
                    .dividedBy(upperPrice)
                    .multipliedBy(hundred);

            if (deviation.isLessThanOrEqual(highThresholdPercentages)) {
                return upperPrice;
            }
        }

        return null;
    }

    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }
}
