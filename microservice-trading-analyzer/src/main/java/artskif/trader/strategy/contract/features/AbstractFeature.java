package artskif.trader.strategy.contract.features;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Абстрактный базовый класс для фич
 * Содержит общую логику для создания метаданных и делегирования базовых методов
 *
 * @param <T> тип индикатора, наследующийся от MultiAbstractIndicator
 */
public abstract class AbstractFeature<T extends MultiAbstractIndicator<? extends AbstractIndicator<Num>>> implements Feature {

    protected final T indicatorM;

    protected AbstractFeature(T indicatorM) {
        this.indicatorM = indicatorM;
    }

    /**
     * Создает список метаданных для указанных типов фич
     *
     * @param featureTypesMap мапа где ключ - sequenceOrder, значение - тип фичи
     * @param contract контракт, к которому относятся метаданные
     * @return список метаданных контракта
     */
    public static List<ContractMetadata> getFeatureMetadata(Map<Integer, FeatureTypeMetadata> featureTypesMap, Contract contract) {
        List<ContractMetadata> metadataList = new ArrayList<>();

        for (Map.Entry<Integer, FeatureTypeMetadata> entry : featureTypesMap.entrySet()) {
            Integer sequenceOrder = entry.getKey();
            FeatureTypeMetadata featureType = entry.getValue();

            metadataList.add(new ContractMetadata(
                featureType.getName(),
                featureType.getDescription(),
                sequenceOrder,
                featureType.getDataType(),
                MetadataType.FEATURE,
                contract
            ));
        }

        return metadataList;
    }

    /**
     * Получить индикатор для указанного таймфрейма с выбором типа серии
     *
     * @param timeframe таймфрейм
     * @param isLiveSeries использовать live серию или historical
     * @return индикатор TA4J
     */
    public AbstractIndicator<Num> getIndicator(CandleTimeframe timeframe, boolean isLiveSeries) {
        return indicatorM.getIndicator(timeframe, isLiveSeries);
    }

    public Num getHigherTimeframeValue(int index, CandleTimeframe lowerTimeframe, CandleTimeframe higherTimeframe, boolean isLifeSeries){
        return indicatorM.getHigherTimeframeValue(index, lowerTimeframe, higherTimeframe, isLifeSeries);
    }

    /**
     * Обобщенная реализация получения значения по имени фичи
     * Использует метаданные enum для определения стратегии получения значения
     *
     * @param isLiveSeries использовать live серию или historical
     * @param valueName имя значения фичи
     * @param index индекс в серии
     * @param featureTypes массив enum значений, реализующих FeatureTypeMetadata
     * @return значение индикатора
     */
    protected Num getValueByNameGeneric(boolean isLiveSeries, String valueName, int index, FeatureTypeMetadata[] featureTypes) {
        FeatureTypeMetadata featureType = FeatureTypeMetadata.findByName(featureTypes, valueName);
        FeatureMetadata metadata = featureType.getMetadata();

        if (metadata.usesHigherTimeframe()) {
            return getHigherTimeframeValue(index, metadata.timeframe(), metadata.higherTimeframe(), isLiveSeries);
        } else {
            return getIndicator(metadata.timeframe(), isLiveSeries).getValue(index);
        }
    }
}

