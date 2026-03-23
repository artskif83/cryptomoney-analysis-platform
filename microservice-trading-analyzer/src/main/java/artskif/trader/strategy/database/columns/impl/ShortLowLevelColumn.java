package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.base.ShortLowLevelIndicator;
import artskif.trader.strategy.indicators.multi.levels.ShortLowLevelIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class ShortLowLevelColumn extends AbstractColumn<ShortLowLevelIndicatorM> {

    /**
     * Перечислимый тип для различных значений уровня поддержки
     */
    public enum ShortLevelColumnType implements ColumnTypeMetadata {
        SHORT_LEVEL_5M(
                "metric_short_level_5m",
                "Уровень поддержки на таймфрейме 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        SHORT_LEVEL_1M(
                "metric_short_level_1m",
                "Уровень поддержки на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        SHORT_LEVEL_STOP_LOS_1M(
                "metric_short_level_stop_los_1m",
                "Стоп лос над уровнем поддержки на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        );

        private final ColumnMetadata metadata;

        ShortLevelColumnType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected ShortLowLevelColumn() {
        super(null);
    }

    @Inject
    public ShortLowLevelColumn(ShortLowLevelIndicatorM shortLowLevelIndicatorM) {
        super(shortLowLevelIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        ColumnTypeMetadata featureType = ColumnTypeMetadata.findByName(ShortLevelColumnType.values(), valueName);
        ColumnMetadata metadata = featureType.getMetadata();

        switch (featureType) {
            case ShortLevelColumnType.SHORT_LEVEL_STOP_LOS_1M:
                ShortLowLevelIndicator indicator = (ShortLowLevelIndicator) getIndicator(metadata.timeframe(), isLiveSeries);
                return indicator.getStopLos(index);
            default:
                return getValueByNameGeneric(isLiveSeries, valueName, index, ShortLevelColumnType.values());
        }
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(ShortLevelColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(ShortLevelColumnType.values(), name);
    }
}
