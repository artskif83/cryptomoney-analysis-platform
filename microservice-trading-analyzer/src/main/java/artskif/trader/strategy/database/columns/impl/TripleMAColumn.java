package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.base.TripleMAIndicator;
import artskif.trader.strategy.indicators.multi.TripleMAIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

/**
 * Колонка для индикатора тройной скользящей средней.
 * Предоставляет доступ к значениям трёх SMA, их углам наклона и основному значению индикатора.
 */
@ApplicationScoped
public class TripleMAColumn extends AbstractColumn<TripleMAIndicatorM> {

    /**
     * Перечислимый тип для различных значений TripleMA фичи
     */
    public enum TripleMAColumnType implements ColumnTypeMetadata {
        // Значения быстрой SMA
        FAST_SMA_1M(
                "metric_triple_ma_fast_sma_1m",
                "Быстрая скользящая средняя (Fast SMA) на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        FAST_SMA_5M(
                "metric_triple_ma_fast_sma_5m",
                "Быстрая скользящая средняя (Fast SMA) на таймфрейме 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        FAST_SMA_4H(
                "metric_triple_ma_fast_sma_4h",
                "Быстрая скользящая средняя (Fast SMA) на таймфрейме 4h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Значения средней SMA
        MEDIUM_SMA_1M(
                "metric_triple_ma_medium_sma_1m",
                "Средняя скользящая средняя (Medium SMA) на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        MEDIUM_SMA_5M(
                "metric_triple_ma_medium_sma_5m",
                "Средняя скользящая средняя (Medium SMA) на таймфрейме 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        MEDIUM_SMA_4H(
                "metric_triple_ma_medium_sma_4h",
                "Средняя скользящая средняя (Medium SMA) на таймфрейме 4h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Значения медленной SMA
        SLOW_SMA_1M(
                "metric_triple_ma_slow_sma_1m",
                "Медленная скользящая средняя (Slow SMA) на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        SLOW_SMA_5M(
                "metric_triple_ma_slow_sma_5m",
                "Медленная скользящая средняя (Slow SMA) на таймфрейме 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        SLOW_SMA_4H(
                "metric_triple_ma_slow_sma_4h",
                "Медленная скользящая средняя (Slow SMA) на таймфрейме 4h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Углы наклона быстрой SMA
        FAST_SMA_ANGLE_1M(
                "metric_triple_ma_fast_angle_1m",
                "Угол наклона быстрой SMA в градусах на таймфрейме 1m",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        FAST_SMA_ANGLE_5M(
                "metric_triple_ma_fast_angle_5m",
                "Угол наклона быстрой SMA в градусах на таймфрейме 5m",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        FAST_SMA_ANGLE_4H(
                "metric_triple_ma_fast_angle_4h",
                "Угол наклона быстрой SMA в градусах на таймфрейме 4h",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Углы наклона средней SMA
        MEDIUM_SMA_ANGLE_1M(
                "metric_triple_ma_medium_angle_1m",
                "Угол наклона средней SMA в градусах на таймфрейме 1m",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        MEDIUM_SMA_ANGLE_5M(
                "metric_triple_ma_medium_angle_5m",
                "Угол наклона средней SMA в градусах на таймфрейме 5m",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        MEDIUM_SMA_ANGLE_4H(
                "metric_triple_ma_medium_angle_4h",
                "Угол наклона средней SMA в градусах на таймфрейме 4h",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Углы наклона медленной SMA
        SLOW_SMA_ANGLE_1M(
                "metric_triple_ma_slow_angle_1m",
                "Угол наклона медленной SMA в градусах на таймфрейме 1m",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        SLOW_SMA_ANGLE_5M(
                "metric_triple_ma_slow_angle_5m",
                "Угол наклона медленной SMA в градусах на таймфрейме 5m",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        SLOW_SMA_ANGLE_4H(
                "metric_triple_ma_slow_angle_4h",
                "Угол наклона медленной SMA в градусах на таймфрейме 4h",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Основное значение индикатора (calculate)
        TRIPLE_MA_VALUE_1M(
                "metric_triple_ma_value_1m",
                "Основное значение индикатора TripleMA (Fast SMA) на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        TRIPLE_MA_VALUE_5M(
                "metric_triple_ma_value_5m",
                "Основное значение индикатора TripleMA (Fast SMA) на таймфрейме 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        TRIPLE_MA_VALUE_4H(
                "metric_triple_ma_value_4h",
                "Основное значение индикатора TripleMA (Fast SMA) на таймфрейме 4h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Значения на старшем таймфрейме
        TRIPLE_MA_VALUE_5M_ON_4H(
                "metric_triple_ma_value_5m_on_4h",
                "Основное значение индикатора TripleMA на таймфрейме 4h для индекса 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                CandleTimeframe.CANDLE_4H,
                MetadataType.METRIC
        ),

        // Значения на старшем таймфрейме
        TRIPLE_MA_VALUE_1M_ON_4H(
                "metric_triple_ma_value_1m_on_4h",
                "Основное значение индикатора TripleMA на таймфрейме 4h для индекса 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_4H,
                MetadataType.METRIC
        );

        private final ColumnMetadata metadata;

        TripleMAColumnType(String name, String description, String dataType,
                           CandleTimeframe timeframe, CandleTimeframe higherTimeframe,
                           MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType,
                    timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected TripleMAColumn() {
        super(null);
    }

    @Inject
    public TripleMAColumn(TripleMAIndicatorM tripleMAIndicatorM) {
        super(tripleMAIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        ColumnTypeMetadata featureType = ColumnTypeMetadata.findByName(
                TripleMAColumnType.values(), valueName);
        ColumnMetadata metadata = featureType.getMetadata();

        // Получаем индикатор для нужного таймфрейма
        TripleMAIndicator indicator = (TripleMAIndicator) getIndicator(
                metadata.timeframe(), isLiveSeries);

        // Определяем, какое значение нужно вернуть
        switch ((TripleMAColumnType) featureType) {
            // Быстрая SMA
            case FAST_SMA_1M:
            case FAST_SMA_5M:
            case FAST_SMA_4H:
                return indicator.getFastSMA(index);

            // Средняя SMA
            case MEDIUM_SMA_1M:
            case MEDIUM_SMA_5M:
            case MEDIUM_SMA_4H:
                return indicator.getMediumSMA(index);

            // Медленная SMA
            case SLOW_SMA_1M:
            case SLOW_SMA_5M:
            case SLOW_SMA_4H:
                return indicator.getSlowSMA(index);

            // Углы наклона быстрой SMA
            case FAST_SMA_ANGLE_1M:
            case FAST_SMA_ANGLE_5M:
            case FAST_SMA_ANGLE_4H:
                return indicator.getFastSMAAngle(index);

            // Углы наклона средней SMA
            case MEDIUM_SMA_ANGLE_1M:
            case MEDIUM_SMA_ANGLE_5M:
            case MEDIUM_SMA_ANGLE_4H:
                return indicator.getMediumSMAAngle(index);

            // Углы наклона медленной SMA
            case SLOW_SMA_ANGLE_1M:
            case SLOW_SMA_ANGLE_5M:
            case SLOW_SMA_ANGLE_4H:
                return indicator.getSlowSMAAngle(index);

            // Основное значение индикатора
            case TRIPLE_MA_VALUE_1M:
            case TRIPLE_MA_VALUE_5M:
            case TRIPLE_MA_VALUE_4H:
                return indicator.getValue(index);

            // Значение на старшем таймфрейме 5m
            case TRIPLE_MA_VALUE_5M_ON_4H:
                return getHigherTimeframeValue(index, metadata.timeframe(),
                        metadata.higherTimeframe(), isLiveSeries);

                // Значение на старшем таймфрейме 1m
            case TRIPLE_MA_VALUE_1M_ON_4H:
                return getHigherTimeframeValue(index, metadata.timeframe(),
                        metadata.higherTimeframe(), isLiveSeries);

            default:
                throw new IllegalArgumentException("Неизвестный тип колонки: " + valueName);
        }
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(TripleMAColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(TripleMAColumnType.values(), name);
    }
}
