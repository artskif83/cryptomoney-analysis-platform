package artskif.trader.strategy.database.columns;

import artskif.trader.candle.CandleTimeframe;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

import java.util.List;

/**
 * Интерфейс для создателей фич (features)
 * Каждый индикатор должен реализовывать этот интерфейс для добавления фич в контракт
 */
public interface Column {


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
    List<String> getColumnNames();

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
    ColumnTypeMetadata getColumnTypeMetadataByName(String name);
}

