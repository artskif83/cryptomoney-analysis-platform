package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.base.ResistanceLevelIndicator;
import artskif.trader.strategy.indicators.multi.levels.ResistanceLevelIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class ResistanceLevelColumn extends AbstractColumn<ResistanceLevelIndicatorM> {

    /**
     * Перечислимый тип для различных значений уровня сопротивления
     */
    public enum ResistanceLevelColumnType implements ColumnTypeMetadata {
        RESISTANCE_LEVEL_5M(
                "metric_resistance_level_5m",
                "Уровень сопротивления на таймфрейме 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        RESISTANCE_LEVEL_1M(
                "metric_resistance_level_1m",
                "Уровень сопротивления на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        RESISTANCE_STOP_LOS_1M(
                "metric_resistance_stop_los_1m",
                "Стоп лос над линией лучшей цены на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        );
        private final ColumnMetadata metadata;

        ResistanceLevelColumnType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected ResistanceLevelColumn() {
        super(null);
    }

    @Inject
    public ResistanceLevelColumn(ResistanceLevelIndicatorM resistanceLevelIndicatorM) {
        super(resistanceLevelIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        ColumnTypeMetadata featureType = ColumnTypeMetadata.findByName(ResistanceLevelColumn.ResistanceLevelColumnType.values(), valueName);
        ColumnMetadata metadata = featureType.getMetadata();

        switch (featureType) {
            case ResistanceLevelColumnType.RESISTANCE_STOP_LOS_1M:
                ResistanceLevelIndicator indicator = (ResistanceLevelIndicator) getIndicator(metadata.timeframe(), isLiveSeries);
                return indicator.getStopLos(index);
            default:
                return getValueByNameGeneric(isLiveSeries, valueName, index, ResistanceLevelColumnType.values());
        }
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(ResistanceLevelColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(ResistanceLevelColumnType.values(), name);
    }
}
