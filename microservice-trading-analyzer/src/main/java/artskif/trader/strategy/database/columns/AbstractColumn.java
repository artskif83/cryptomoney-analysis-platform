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
 * Абстрактный базовый класс для фич
 * Содержит общую логику для создания метаданных и делегирования базовых методов
 *
 * @param <T> тип индикатора, наследующийся от MultiAbstractIndicator
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

    public Num getHigherTimeframeValue(int index, CandleTimeframe lowerTimeframe, CandleTimeframe higherTimeframe, boolean isLifeSeries) {
        return indicatorM.getHigherTimeframeValue(index, lowerTimeframe, higherTimeframe, isLifeSeries);
    }

    /**
     * Обобщенная реализация получения значения по имени фичи
     * Использует метаданные enum для определения стратегии получения значения
     *
     * @param isLiveSeries использовать live серию или historical
     * @param valueName    имя значения фичи
     * @param index        индекс в серии
     * @param featureTypes массив enum значений, реализующих FeatureTypeMetadata
     * @return значение индикатора
     */
    protected Num getValueByNameGeneric(boolean isLiveSeries, String valueName, int index, ColumnTypeMetadata[] featureTypes) {
        ColumnTypeMetadata featureType = ColumnTypeMetadata.findByName(featureTypes, valueName);
        ColumnMetadata metadata = featureType.getMetadata();

        if (metadata.usesHigherTimeframe()) {
            return getHigherTimeframeValue(index, metadata.timeframe(), metadata.higherTimeframe(), isLiveSeries);
        } else {
            return getIndicator(metadata.timeframe(), isLiveSeries).getValue(index);
        }
    }
}

