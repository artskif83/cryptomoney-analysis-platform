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
     * Получить исторические данные свечей для расчета фичи
     *
     * @return список DTO свечей
     */
    List<CandlestickDto> getCandlestickDtos(CandleTimeframe timeframe);

    /**
     * Получить индикатор TA4J для расчета фичи
     *
     * @return индикатор TA4J
     */
    AbstractIndicator<Num> getIndicator(CandleTimeframe timeframe);

    /**
     * Получить имена значений фичи для сохранения в БД
     *
     * @return список имен значений фичи
     */
    List<String> getFeatureValueNames();

    /**
     * Получить значение фичи по имени
     *
     * @return значение фичи
     */
    Num getValueByName(String valueName, int index);

    /**
     * Получить тип данных фичи
     *
     * @return тип данных в БД
     */
    FeatureTypeMetadata getFeatureTypeMetadataByValueName(String name);
}

