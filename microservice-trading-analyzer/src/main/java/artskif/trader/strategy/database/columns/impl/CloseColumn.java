package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.indicators.multi.CloseIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class CloseColumn extends AbstractColumn<CloseIndicatorM> {

    /**
     * Перечислимый тип для различных значений ADX фичи
     */
    public enum CloseColumnType implements ColumnTypeMetadata {
        BASE_5M(
                "metric_close_5m",
                "Close индикатор на таймфрейме 5m",
                "numeric(18, 2)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        BASE_4H(
                "metric_close_4h",
                "Close индикатор на таймфрейме 4h",
                "numeric(18, 2)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        );

        private final ColumnMetadata metadata;

        CloseColumnType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected CloseColumn() {
        super(null);
    }

    @Inject
    public CloseColumn(CloseIndicatorM closeIndicatorM) {
        super(closeIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        return getValueByNameGeneric(isLiveSeries, valueName, index, CloseColumnType.values());
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(CloseColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(CloseColumnType.values(), name);
    }
}
