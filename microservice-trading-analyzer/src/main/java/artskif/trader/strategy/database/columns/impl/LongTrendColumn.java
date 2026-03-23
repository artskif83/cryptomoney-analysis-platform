package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.base.LongTrendIndicator;
import artskif.trader.strategy.indicators.multi.levels.LongTrendIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class LongTrendColumn extends AbstractColumn<LongTrendIndicatorM> {

    /**
     * Перечислимый тип для различных значений уровня поддержки
     */
    public enum LongTrendColumnType implements ColumnTypeMetadata {
        LONG_TREND_5M(
                "metric_long_level_5m",
                "Уровень поддержки на таймфрейме 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        LONG_TREND_1M(
                "metric_long_trend_1m",
                "Уровень поддержки на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        LONG_STOP_LOS_1M(
                "metric_long_stop_los_1m",
                "Стоп лос под линией лучшей цены на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        );

        private final ColumnMetadata metadata;

        LongTrendColumnType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected LongTrendColumn() {
        super(null);
    }

    @Inject
    public LongTrendColumn(LongTrendIndicatorM longTrendIndicatorM) {
        super(longTrendIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        ColumnTypeMetadata featureType = ColumnTypeMetadata.findByName(LongTrendColumnType.values(), valueName);
        ColumnMetadata metadata = featureType.getMetadata();

        switch (featureType) {
            case LongTrendColumnType.LONG_STOP_LOS_1M:
                LongTrendIndicator indicator = (LongTrendIndicator) getIndicator(metadata.timeframe(), isLiveSeries);
                return indicator.getStopLos(index);
            default:
                return getValueByNameGeneric(isLiveSeries, valueName, index, LongTrendColumnType.values());
        }
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(LongTrendColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(LongTrendColumnType.values(), name);
    }
}
