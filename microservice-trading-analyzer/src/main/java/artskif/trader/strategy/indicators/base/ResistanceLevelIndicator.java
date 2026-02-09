package artskif.trader.strategy.indicators.base;

import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ResistanceLevelIndicator  extends CachedIndicator<Num> {

    private final HighPriceIndicator highPriceIndicator;
    private final CandleResistanceStrength candleResistanceStrength;
    private final int barCount;
    private final Num resistanceRangePercentagesThreshold;

    public ResistanceLevelIndicator(HighPriceIndicator highPriceIndicator,
                                    CandleResistanceStrength candleResistanceStrength) {
        this(highPriceIndicator, candleResistanceStrength, 32, DecimalNum.valueOf(0.002));

    }

    public ResistanceLevelIndicator(HighPriceIndicator highPriceIndicator,
                                    CandleResistanceStrength candleResistanceStrength,
                                    int barCount, Num resistanceRangePercentagesThreshold) {
        super(highPriceIndicator);
        this.highPriceIndicator = highPriceIndicator;
        this.candleResistanceStrength = candleResistanceStrength;
        this.barCount = barCount;
        this.resistanceRangePercentagesThreshold = resistanceRangePercentagesThreshold;
    }

    @Override
    protected Num calculate(int index) {
        List<PriceWithIndex> sortedPrices = sortByHighPrice(highPriceIndicator, candleResistanceStrength, index);

        return resistancePower(sortedPrices,
                resistanceRangePercentagesThreshold, highPriceIndicator.getValue(index));
    }

    /**
     * Вычисляет максимальную силу сопротивления для окон, содержащих текущую цену currentPrice.
     * Алгоритм:
     * 1. Берем каждый элемент из списка как верхнюю границу окна
     * 2. Вычисляем окно размером resistanceRangePercentagesThreshold
     * 3. Если currentPrice попадает в это окно, суммируем силу сопротивления всех элементов в окне
     * 4. Возвращаем максимальную найд��нную силу
     *
     * @param sortedPrices отсортированный список цен (от максимальной к минимальной) с силой сопротивления
     * @param resistanceRangePercentagesThreshold процентный порог для определения размера окна
     * @param currentPrice текущая цена, которая должна попасть в окно
     * @return максимальная суммарная сила сопротивления среди всех окон, содержащих currentPrice
     */
    Num resistancePower(List<PriceWithIndex> sortedPrices, Num resistanceRangePercentagesThreshold, Num currentPrice) {
        if (sortedPrices.isEmpty()) {
            return getBarSeries().numFactory().zero();
        }

        Num maxResistancePower = getBarSeries().numFactory().zero();

        // Проходим по списку сверху вниз (от максимальной цены к минимальной)
        for (int i = 0; i < sortedPrices.size(); i++) {
            PriceWithIndex topLevel = sortedPrices.get(i);
            Num topPrice = topLevel.getPrice();

            // Вычисляем нижнюю границу окна: topPrice * (1 - resistanceRangePercentagesThreshold)
            Num lowerBound = topPrice.multipliedBy(
                getBarSeries().numFactory().one().minus(resistanceRangePercentagesThreshold)
            );

            // Проверяем, попадает ли currentPrice в окно [lowerBound, topPrice]
            if (currentPrice.isGreaterThanOrEqual(lowerBound) && currentPrice.isLessThanOrEqual(topPrice)) {
                // currentPrice попадает в окно - суммируем силу сопротивления всех элементов в этом окне
                Num totalResistance = getBarSeries().numFactory().zero();

                // Проходим по всем элементам и суммируем те, что попадают в окно
                for (int j = i; j < sortedPrices.size(); j++) {
                    PriceWithIndex priceItem = sortedPrices.get(j);
                    Num price = priceItem.getPrice();

                    // Проверяем, попадает ли цена в окно [lowerBound, topPrice]
                    if (price.isGreaterThanOrEqual(lowerBound) && price.isLessThanOrEqual(topPrice)) {
                        totalResistance = totalResistance.plus(priceItem.getResistanceStrength());
                    } else if (price.isLessThan(lowerBound)) {
                        // Цена вышла за нижнюю границу окна, дальше проверять нет смысла
                        break;
                    }
                }

                // Обновляем максимальную силу сопротивления, если нашли большее значение
                if (totalResistance.isGreaterThan(maxResistancePower)) {
                    maxResistancePower = totalResistance;
                }
            }
        }

        return maxResistancePower;
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
        for (int i = startIndex; i <= currentIndex; i++) {
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
    }

    @Override
    public int getCountOfUnstableBars() {
        return 0;
    }
}
