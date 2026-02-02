package artskif.trader.strategy.indicators;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleInstance;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.util.IndicatorUtils;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.NaN;
import org.ta4j.core.num.Num;

import java.util.HashMap;
import java.util.Map;

public abstract class MultiAbstractIndicator<T extends AbstractIndicator<Num>> {

    protected final Candle candle;
    protected final Map<CandleTimeframe, T> indicators = new HashMap<>();

    public MultiAbstractIndicator(Candle candle) {
        this.candle = candle;
    }

    /**
     * Получить свечи для расчета индикатора
     *
     * @return BarSeries для указанного таймфрейма
     */
    protected BarSeries getBarSeries(CandleTimeframe timeframe, boolean isLifeSeries) {
        if (!candle.hasInstance(timeframe)) {
            throw new IllegalArgumentException("Candle instance for timeframe " + timeframe + " does not exist.");
        }
        CandleInstance candleInstance = candle.getInstance(timeframe);
        return isLifeSeries ? candleInstance.getLiveBarSeries() : candleInstance.getHistoricalBarSeries();
    }

    /**
     * Создать индикатор для указанного таймфрейма.
     * Этот метод должен быть реализован в подклассах.
     *
     * @param timeframe таймфрейм для создания индикатора
     * @param isLifeSeries флаг для выбора между live и historical сериями
     * @return созданный индикатор типа T
     */
    protected abstract T createIndicator(CandleTimeframe timeframe, boolean isLifeSeries);

    /**
     * Получить индикатор TA4J для указанного таймфрейма.
     * Если индикатор для данного таймфрейма уже существует в кэше, возвращает его,
     * иначе создает новый и сохраняет в кэш.
     *
     * @param timeframe таймфрейм для получения индикатора
     * @param isLifeSeries флаг для выбора между live и historical сериями
     * @return индикатор TA4J для указанного таймфрейма
     */
    public T getIndicator(CandleTimeframe timeframe, boolean isLifeSeries){
        if (indicators.containsKey(timeframe)) {
            return indicators.get(timeframe);
        } else {
            T indicator = createIndicator(timeframe, isLifeSeries);
            this.indicators.put(timeframe, indicator);
            return indicator;
        }
    }

    /**
     * Получить значение индикатора на старшем таймфрейме
     * Реализация по умолчанию использует FeaturesUtils для маппинга индексов
     *
     * @return значение фичи
     */
    public Num getHigherTimeframeValue(int index, CandleTimeframe lowerTimeframe, CandleTimeframe higherTimeframe, boolean isLifeSeries) {
        AbstractIndicator<Num> lowerTfIndicator = getIndicator(lowerTimeframe, isLifeSeries);
        AbstractIndicator<Num> higherTfIndicator = getIndicator(higherTimeframe, isLifeSeries);
        int higherTfIndex = IndicatorUtils.mapToHigherTfIndex(lowerTfIndicator.getBarSeries().getBar(index), higherTfIndicator.getBarSeries());
        return higherTfIndex == -1 ? NaN.NaN : higherTfIndicator.getValue(higherTfIndex);
    }

}
