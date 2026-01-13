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
    CandleTimeframe timeframe
) {
}

