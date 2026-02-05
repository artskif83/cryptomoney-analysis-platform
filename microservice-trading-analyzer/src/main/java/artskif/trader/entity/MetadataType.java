package artskif.trader.entity;

/**
 * Тип метаданных контракта
 */
public enum MetadataType {
    /**
     * Фича (признак для ML модели)
     */
    FEATURE,

    /**
     * Лейбл (целевая переменная)
     */
    LABEL,

    /**
     * Метрика для отображения
     */
    METRIC,

    /**
     * Дополнительная информация
     */
    ADDITIONAL
}

