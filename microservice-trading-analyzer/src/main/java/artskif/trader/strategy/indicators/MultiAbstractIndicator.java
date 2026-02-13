package artskif.trader.strategy.indicators;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleInstance;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.base.HigherTimeframeIndicator;
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
    protected final Map<TimeframesPair, AbstractIndicator<Num>> higherTimeframeIndicators = new HashMap<>();

    public MultiAbstractIndicator(Candle candle) {
        this.candle = candle;
    }

    /**
     * Вложенный класс для хранения пары таймфреймов в качестве ключа кэша
     */
    protected static class TimeframesPair {
        private final CandleTimeframe lowerTimeframe;
        private final CandleTimeframe higherTimeframe;
        private final boolean isLifeSeries;

        public TimeframesPair(CandleTimeframe lowerTimeframe, CandleTimeframe higherTimeframe, boolean isLifeSeries) {
            this.lowerTimeframe = lowerTimeframe;
            this.higherTimeframe = higherTimeframe;
            this.isLifeSeries = isLifeSeries;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimeframesPair that = (TimeframesPair) o;
            return isLifeSeries == that.isLifeSeries &&
                    lowerTimeframe == that.lowerTimeframe &&
                    higherTimeframe == that.higherTimeframe;
        }

        @Override
        public int hashCode() {
            int result = lowerTimeframe.hashCode();
            result = 31 * result + higherTimeframe.hashCode();
            result = 31 * result + (isLifeSeries ? 1 : 0);
            return result;
        }
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
     * Получить индикатор для более высокого таймфрейма, который использует данные с более низкого таймфрейма.
     * Если такой индикатор уже существует в кэше, возвращает его, иначе создает новый и сохраняет в кэш.
     *
     * @param lowerTimeframe  нижний таймфрейм
     * @param higherTimeframe верхний таймфрейм
     * @param isLifeSeries флаг для выбора между live и historical сериями
     * @return индикатор для более высокого таймфрейма
     */
    public AbstractIndicator<Num> getHigherTimeframeIndicator(CandleTimeframe lowerTimeframe, CandleTimeframe higherTimeframe, boolean isLifeSeries){
        TimeframesPair key = new TimeframesPair(lowerTimeframe, higherTimeframe, isLifeSeries);

        if (higherTimeframeIndicators.containsKey(key)) {
            return higherTimeframeIndicators.get(key);
        } else {
            T lowerTimeframeIndicator = getIndicator(lowerTimeframe, isLifeSeries);
            T higherTimeframeIndicator = getIndicator(higherTimeframe, isLifeSeries);

            AbstractIndicator<Num> indicator = new HigherTimeframeIndicator(lowerTimeframeIndicator, higherTimeframeIndicator);
            higherTimeframeIndicators.put(key, indicator);
            return indicator;
        }
    }
}
