package artskif.trader.strategy.database.columns;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;

/**
 * Record для хранения метаданных фичи
 * Используется внутри enum для композиции общих полей
 */
public record ColumnMetadata(
        String name,
        String description,
        String dataType,
        MetadataType metadataType,
        CandleTimeframe timeframe,
        CandleTimeframe higherTimeframe  // null если не используется higher timeframe
) {
    /**
     * Конструктор для обычных фич без higher timeframe
     */
    public ColumnMetadata(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
        this(name, description, dataType, metadataType, timeframe, higherTimeframe);
    }

    /**
     * Проверяет, использует ли фича значение с higher timeframe
     */
    public boolean usesHigherTimeframe() {
        return higherTimeframe != null;
    }
}

