package artskif.trader.strategy.database.columns;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.List;

/**
 * Абстрактный базовый класс для колонок (фич) в базе данных
 * <p>
 * Предоставляет общую функциональность для работы с индикаторами технического анализа:
 * - Получение значений индикаторов для различных таймфреймов
 * - Создание метаданных для колонок
 * - Работа с live и historical сериями данных
 *
 * @param <T> тип мульти-индикатора, который используется для получения значений
 */
public abstract class AbstractColumn<T extends MultiAbstractIndicator<? extends AbstractIndicator<Num>>> implements Column {

    protected final T indicatorM;

    protected AbstractColumn(T indicatorM) {
        this.indicatorM = indicatorM;
    }

    /**
     * Создает список метаданных для указанных типов фич
     *
     * @param featureTypesList список типов фич (sequenceOrder определяется автоматически по порядку в списке, начиная с 1)
     * @param contract         контракт, к которому относятся метаданные
     * @return список метаданных контракта
     */
    public static List<ContractMetadata> getColumnMetadata(List<ColumnTypeMetadata> featureTypesList, Contract contract) {
        List<ContractMetadata> metadataList = new ArrayList<>();

        for (int i = 0; i < featureTypesList.size(); i++) {
            Integer sequenceOrder = i + 1; // sequenceOrder начинается с 1
            ColumnTypeMetadata featureType = featureTypesList.get(i);

            metadataList.add(new ContractMetadata(
                    featureType.getName(),
                    featureType.getDescription(),
                    sequenceOrder,
                    featureType.getDataType(),
                    featureType.getMetadataType(),
                    contract
            ));
        }

        return metadataList;
    }

    /**
     * Получить индикатор для указанного таймфрейма с выбором типа серии
     *
     * @param timeframe    таймфрейм
     * @param isLiveSeries использовать live серию или historical
     * @return индикатор TA4J
     */
    public AbstractIndicator<Num> getIndicator(CandleTimeframe timeframe, boolean isLiveSeries) {
        return indicatorM.getIndicator(timeframe, isLiveSeries);
    }

    /**
     * Получить значение индикатора на старшем таймфрейме для указанного индекса на младшем таймфрейме
     *
     * @param index          индекс на младшем таймфрейме
     * @param lowerTimeframe младший таймфрейм
     * @param higherTimeframe старший таймфрейм
     * @param isLifeSeries   использовать live серию или historical
     * @return значение индикатора на старшем таймфрейме
     */
    public Num getHigherTimeframeValue(int index, CandleTimeframe lowerTimeframe, CandleTimeframe higherTimeframe, boolean isLifeSeries) {
        AbstractIndicator<Num> higherTimeframeIndicator = indicatorM.getHigherTimeframeIndicator(lowerTimeframe, higherTimeframe, isLifeSeries);
        return higherTimeframeIndicator.getValue(index);
    }

    /**
     * Обобщенная реализация получения значения по имени фичи
     * Использует метаданные enum для определения стратегии получения значения
     *
     * @param isLiveSeries использовать live серию или historical
     * @param valueName    имя значения фичи
     * @param index        индекс в серии
     * @param columnTypeMetadata массив enum значений, реализующих FeatureTypeMetadata
     * @return значение индикатора
     */
    protected Num getValueByNameGeneric(boolean isLiveSeries, String valueName, int index, ColumnTypeMetadata[] columnTypeMetadata) {
        ColumnTypeMetadata featureType = ColumnTypeMetadata.findByName(columnTypeMetadata, valueName);
        ColumnMetadata metadata = featureType.getMetadata();

        if (metadata.usesHigherTimeframe()) {
            return getHigherTimeframeValue(index, metadata.timeframe(), metadata.higherTimeframe(), isLiveSeries);
        } else {
            return getIndicator(metadata.timeframe(), isLiveSeries).getValue(index);
        }
    }
}

