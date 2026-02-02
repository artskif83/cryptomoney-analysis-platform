package artskif.trader.strategy.contract.features;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.util.IndicatorUtils;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.NaN;
import org.ta4j.core.num.Num;

import java.util.List;

/**
 * Интерфейс для создателей фич (features)
 * Каждый индикатор должен реализовывать этот интерфейс для добавления фич в контракт
 */
public interface Feature {


    /**
     * Получить индикатор TA4J для расчета фичи
     *
     * @return индикатор TA4J
     */
    AbstractIndicator<Num> getIndicator(CandleTimeframe timeframe, boolean isLiveSeries);

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
    Num getValueByName(boolean isLiveSeries, String valueName, int index);

    /**
     * Получить тип данных фичи
     *
     * @return тип данных в БД
     */
    FeatureTypeMetadata getFeatureTypeMetadataByValueName(String name);
}

