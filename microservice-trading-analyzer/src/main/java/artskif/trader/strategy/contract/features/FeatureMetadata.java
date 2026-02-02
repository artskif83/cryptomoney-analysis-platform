package artskif.trader.strategy.contract.features;

import artskif.trader.candle.CandleTimeframe;

/**
 * Record для хранения метаданных фичи
 * Используется внутри enum для композиции общих полей
 */
public record FeatureMetadata(
    String name,
    String description,
    String dataType,
    CandleTimeframe timeframe,
    CandleTimeframe higherTimeframe  // null если не используется higher timeframe
) {
    /**
     * Конструктор для обычных фич без higher timeframe
     */
    public FeatureMetadata(String name, String description, String dataType, CandleTimeframe timeframe) {
        this(name, description, dataType, timeframe, null);
    }

    /**
     * Проверяет, использует ли фича значение с higher timeframe
     */
    public boolean usesHigherTimeframe() {
        return higherTimeframe != null;
    }
}

