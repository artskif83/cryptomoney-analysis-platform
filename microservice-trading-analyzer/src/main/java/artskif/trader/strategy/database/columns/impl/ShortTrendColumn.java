package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.base.ShortTrendIndicator;
import artskif.trader.strategy.indicators.multi.levels.ShortTrendIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class ShortTrendColumn extends AbstractColumn<ShortTrendIndicatorM> {

    /**
     * Перечислимый тип для различных значений уровня сопротивления
     */
    public enum ShortTrendColumnType implements ColumnTypeMetadata {
        SHORT_TREND_5M(
                "metric_short_trend_5m",
                "Уровень сопротивления на таймфрейме 5m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        SHORT_TREND_1M(
                "metric_short_trend_1m",
                "Уровень сопротивления на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        ),
        SHORT_STOP_LOS_1M(
                "metric_short_stop_los_1m",
                "Стоп лос над линией лучшей цены на таймфрейме 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                null,
                MetadataType.METRIC
        );
        private final ColumnMetadata metadata;

        ShortTrendColumnType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected ShortTrendColumn() {
        super(null);
    }

    @Inject
    public ShortTrendColumn(ShortTrendIndicatorM shortTrendIndicatorM) {
        super(shortTrendIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        ColumnTypeMetadata featureType = ColumnTypeMetadata.findByName(ShortTrendColumnType.values(), valueName);
        ColumnMetadata metadata = featureType.getMetadata();

        switch (featureType) {
            case ShortTrendColumnType.SHORT_STOP_LOS_1M:
                ShortTrendIndicator indicator = (ShortTrendIndicator) getIndicator(metadata.timeframe(), isLiveSeries);
                return indicator.getStopLos(index);
            default:
                return getValueByNameGeneric(isLiveSeries, valueName, index, ShortTrendColumnType.values());
        }
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(ShortTrendColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(ShortTrendColumnType.values(), name);
    }
}
