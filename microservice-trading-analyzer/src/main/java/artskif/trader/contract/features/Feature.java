package artskif.trader.contract.features;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.contract.util.FeaturesUtils;
import artskif.trader.dto.CandlestickDto;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

import java.util.List;

/**
 * Интерфейс для создателей фич (features)
 * Каждый индикатор должен реализовывать этот интерфейс для добавления фич в контракт
 */
public interface Feature {


    /**
     * Получить значение фичи на старшем таймфрейме
     * Реализация по умолчанию использует FeaturesUtils для маппинга индексов
     *
     * @return значение фичи
     */
    default Num getHigherTimeframeValue(int index, CandleTimeframe lowerTimeframe, CandleTimeframe higherTimeframe) {
        AbstractIndicator<Num> lowerTfIndicator = getIndicator(lowerTimeframe);
        AbstractIndicator<Num> higherTfIndicator = getIndicator(higherTimeframe);
        int higherTfIndex = FeaturesUtils.mapToHigherTfIndex(lowerTfIndicator.getBarSeries().getBar(index), higherTfIndicator.getBarSeries());
        return higherTfIndicator.getValue(higherTfIndex);
    }

    /**
     * Получить список свечей для расчета фичи
     *
     * @return список свечей
     */
    List<CandlestickDto> getCandlestickDtos();

    /**
     * Получить индикатор TA4J для расчета фичи
     *
     * @return индикатор TA4J
     */
    AbstractIndicator<Num> getIndicator(CandleTimeframe timeframe);

    /**
     * Получить имя фичи
     *
     * @return имя фичи в БД
     */
    String getFeatureName();

    /**
     * Получить тип данных фичи для создания колонки в БД
     *
     * @return SQL тип данных
     */
    String getDataType();
}

