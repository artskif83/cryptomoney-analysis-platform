package artskif.trader.strategy.contract.features;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.features.impl.BaseFeature;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.entity.MetadataType;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Абстрактный базовый класс для фич
 * Содержит общую логику для создания метаданных и делегирования базовых методов
 *
 * @param <T> тип индикатора, наследующийся от AbstractIndicator
 */
public abstract class AbstractFeature<T extends AbstractIndicator<Num>> implements Feature {

    protected final BaseFeature baseFeature;
    protected final Map<CandleTimeframe, T> indicators = new HashMap<>();

    /**
     * Конструктор для фич, которые используют BaseFeature
     *
     * @param baseFeature базовая фича с OHLCV данными
     */
    protected AbstractFeature(BaseFeature baseFeature) {
        this.baseFeature = baseFeature;
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
     * Делегирует получение свечных данных в BaseFeature
     *
     * @param timeframe таймфрейм для получения данных
     * @return список DTO свечей
     */
    @Override
    public List<CandlestickDto> getCandlestickDtos(CandleTimeframe timeframe) {
        return baseFeature.getCandlestickDtos(timeframe);
    }

    /**
     * Получить индикатор для указанного таймфрейма
     *
     * @param timeframe таймфрейм
     * @return индикатор TA4J
     */
    @Override
    public AbstractIndicator<Num> getIndicator(CandleTimeframe timeframe) {
        return indicators.get(timeframe);
    }
}

