package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.base.DoubleMAIndicator;
import artskif.trader.strategy.indicators.multi.DoubleMAIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

/**
 * Колонка для индикатора двойной скользящей средней.
 * Предоставляет доступ к значениям двух SMA, их углам наклона и основному значению индикатора.
 */
@ApplicationScoped
public class DoubleMAIndicatorColumn extends AbstractColumn<DoubleMAIndicatorM> {

    /**
     * Перечислимый тип для различных значений DoubleMA фичи
     */
    public enum DoubleMAColumnType implements ColumnTypeMetadata {
        // Значения быстрой SMA
        FAST_SMA_1M(
                "metric_double_ma_fast_sma_1m",
                "Быстрая скользящая средняя (Fast SMA) DoubleMA на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        FAST_SMA_5M(
                "metric_double_ma_fast_sma_5m",
                "Быстрая скользящая средняя (Fast SMA) DoubleMA на таймфрейме 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        FAST_SMA_4H(
                "metric_double_ma_fast_sma_4h",
                "Быстрая скользящая средняя (Fast SMA) DoubleMA на таймфрейме 4h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Значения средней SMA
        MEDIUM_SMA_1M(
                "metric_double_ma_medium_sma_1m",
                "Средняя скользящая средняя (Medium SMA) DoubleMA на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        MEDIUM_SMA_5M(
                "metric_double_ma_medium_sma_5m",
                "Средняя скользящая средняя (Medium SMA) DoubleMA на таймфрейме 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        MEDIUM_SMA_4H(
                "metric_double_ma_medium_sma_4h",
                "Средняя скользящая средняя (Medium SMA) DoubleMA на таймфрейме 4h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Углы наклона быстрой SMA
        FAST_SMA_ANGLE_1M(
                "metric_double_ma_fast_angle_1m",
                "Угол наклона быстрой SMA DoubleMA в градусах на таймфрейме 1m",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        FAST_SMA_ANGLE_5M(
                "metric_double_ma_fast_angle_5m",
                "Угол наклона быстрой SMA DoubleMA в градусах на таймфрейме 5m",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        FAST_SMA_ANGLE_4H(
                "metric_double_ma_fast_angle_4h",
                "Угол наклона быстрой SMA DoubleMA в градусах на таймфрейме 4h",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Углы наклона средней SMA
        MEDIUM_SMA_ANGLE_1M(
                "metric_double_ma_medium_angle_1m",
                "Угол наклона средней SMA DoubleMA в градусах на таймфрейме 1m",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        MEDIUM_SMA_ANGLE_5M(
                "metric_double_ma_medium_angle_5m",
                "Угол наклона средней SMA DoubleMA в градусах на таймфрейме 5m",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        MEDIUM_SMA_ANGLE_4H(
                "metric_double_ma_medium_angle_4h",
                "Угол наклона средней SMA DoubleMA в градусах на таймфрейме 4h",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Основное значение индикатора (calculate)
        DOUBLE_MA_VALUE_1M(
                "metric_double_ma_value_1m",
                "Основное значение индикатора DoubleMA (Fast SMA) на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        DOUBLE_MA_VALUE_5M(
                "metric_double_ma_value_5m",
                "Основное значение индикатора DoubleMA (Fast SMA) на таймфрейме 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        DOUBLE_MA_VALUE_4H(
                "metric_double_ma_value_4h",
                "Основное значение индикатора DoubleMA (Fast SMA) на таймфрейме 4h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),

        // Значения на старшем таймфрейме
        DOUBLE_MA_VALUE_5M_ON_4H(
                "metric_double_ma_value_5m_on_4h",
                "Основное значение индикатора DoubleMA на таймфрейме 4h для индекса 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                CandleTimeframe.CANDLE_4H,
                MetadataType.METRIC
        ),

        DOUBLE_MA_VALUE_1M_ON_4H(
                "metric_double_ma_value_1m_on_4h",
                "Основное значение индикатора DoubleMA на таймфрейме 4h для индекса 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_4H,
                MetadataType.METRIC
        ),


        DOUBLE_MA_VALUE_1M_ON_1H(
                "metric_double_ma_value_1m_on_1h",
                "Основное значение индикатора DoubleMA на таймфрейме 1h для индекса 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_1H,
                MetadataType.METRIC
        ),

        DOUBLE_MA_VALUE_1M_ON_5M(
                "metric_double_ma_value_1m_on_5m",
                "Основное значение индикатора DoubleMA на таймфрейме 5m для индекса 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_5M,
                MetadataType.METRIC
        );
        private final ColumnMetadata metadata;

        DoubleMAColumnType(String name, String description, String dataType,
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
    protected DoubleMAIndicatorColumn() {
        super(null);
    }

    @Inject
    public DoubleMAIndicatorColumn(DoubleMAIndicatorM doubleMAIndicatorM) {
        super(doubleMAIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        ColumnTypeMetadata featureType = ColumnTypeMetadata.findByName(
                DoubleMAColumnType.values(), valueName);
        ColumnMetadata metadata = featureType.getMetadata();

        // Получаем индикатор для нужного таймфрейма
        DoubleMAIndicator indicator = (DoubleMAIndicator) getIndicator(
                metadata.timeframe(), isLiveSeries);

        // Определяем, какое значение нужно вернуть
        switch ((DoubleMAColumnType) featureType) {
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

            // Основное значение индикатора
            case DOUBLE_MA_VALUE_1M:
            case DOUBLE_MA_VALUE_5M:
            case DOUBLE_MA_VALUE_4H:
                return indicator.getValue(index);

            // Значение на старшем таймфрейме 5m
            case DOUBLE_MA_VALUE_5M_ON_4H:
                return getHigherTimeframeValue(index, metadata.timeframe(),
                        metadata.higherTimeframe(), isLiveSeries);

            // Значение на старшем таймфрейме 1m
            case DOUBLE_MA_VALUE_1M_ON_4H:
            case DOUBLE_MA_VALUE_1M_ON_1H:
            case DOUBLE_MA_VALUE_1M_ON_5M:
                return getHigherTimeframeValue(index, metadata.timeframe(),
                        metadata.higherTimeframe(), isLiveSeries);

            default:
                throw new IllegalArgumentException("Неизвестный тип колонки: " + valueName);
        }
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(DoubleMAColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(DoubleMAColumnType.values(), name);
    }
}
