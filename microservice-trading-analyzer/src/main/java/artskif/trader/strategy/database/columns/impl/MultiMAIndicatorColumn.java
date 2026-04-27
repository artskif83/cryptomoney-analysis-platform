package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.multi.MultiMAIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

/**
 * Колонка для индикатора MultiMA скользящей средней.
 * Предоставляет доступ к значениям индикатора на различных таймфреймах.
 */
@ApplicationScoped
public class MultiMAIndicatorColumn extends AbstractColumn<MultiMAIndicatorM> {

    /**
     * Перечислимый тип для различных значений MultiMA фичи
     */
    public enum MultiMAColumnType implements ColumnTypeMetadata {

        MULTI_MA_VALUE_1M_ON_1H(
                "metric_double_ma_value_1m_on_1h",
                "Основное значение индикатора MultiMA на таймфрейме 1h для индекса 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_1H,
                MetadataType.METRIC
        ),

        MULTI_MA_VALUE_1M_ON_5M(
                "metric_double_ma_value_1m_on_5m",
                "Основное значение индикатора MultiMA на таймфрейме 5m для индекса 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_5M,
                MetadataType.METRIC
        ),

        MULTI_MA_VALUE_1M_ON_1W(
                "metric_double_ma_value_1m_on_1w",
                "Основное значение индикатора MultiMA на таймфрейме 1w для индекса 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_1W,
                MetadataType.METRIC
        );

        private final ColumnMetadata metadata;

        MultiMAColumnType(String name, String description, String dataType,
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
    protected MultiMAIndicatorColumn() {
        super(null);
    }

    @Inject
    public MultiMAIndicatorColumn(MultiMAIndicatorM multiMAIndicatorM) {
        super(multiMAIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        MultiMAColumnType featureType = ColumnTypeMetadata.findByName(
                MultiMAColumnType.values(), valueName);
        ColumnMetadata metadata = featureType.getMetadata();

        return switch (featureType) {
            case MULTI_MA_VALUE_1M_ON_1H,
                 MULTI_MA_VALUE_1M_ON_5M,
                 MULTI_MA_VALUE_1M_ON_1W -> getHigherTimeframeValue(index, metadata.timeframe(),
                    metadata.higherTimeframe(), isLiveSeries);
        };
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(MultiMAColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(MultiMAColumnType.values(), name);
    }
}

