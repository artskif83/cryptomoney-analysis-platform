package artskif.trader.strategy.database.columns;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;

/**
 * Интерфейс для типов фич, содержащих метаданные
 */
public interface ColumnTypeMetadata {

    ColumnMetadata getMetadata();

    default String getName() {
        return getMetadata().name();
    }

    default MetadataType getMetadataType() {
        return getMetadata().metadataType();
    }

    default String getDescription() {
        return getMetadata().description();
    }

    default String getDataType() {
        return getMetadata().dataType();
    }

    default CandleTimeframe getTimeframe() {
        return getMetadata().timeframe();
    }

    /**
     * Утилитный метод для поиска типа фичи по имени
     *
     * @param values массив значений enum (например, RSIFeatureType.values())
     * @param name имя фичи для поиска
     * @param <T> тип enum, реализующий FeatureTypeMetadata
     * @return найденный тип фичи
     * @throws IllegalArgumentException если тип фичи с указанным именем не найден
     */
    static <T extends ColumnTypeMetadata> T findByName(T[] values, String name) {
        for (T type : values) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown feature name: " + name);
    }

    /**
     * Утилитный метод для получения списка имен всех типов фич
     *
     * @param values массив значений enum (например, RSIFeatureType.values())
     * @param <T> тип enum, реализующий FeatureTypeMetadata
     * @return список имен всех типов фич
     */
    static <T extends ColumnTypeMetadata> java.util.List<String> getNames(T[] values) {
        return java.util.Arrays.stream(values)
            .map(ColumnTypeMetadata::getName)
            .toList();
    }
}

