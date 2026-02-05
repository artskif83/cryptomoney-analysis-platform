package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.multi.CloseIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class PositionColumn extends AbstractColumn<CloseIndicatorM> {

    /**
     * Перечислимый тип для различных значений текущей позиции
     */
    public enum PositionColumnType implements ColumnTypeMetadata {
        STOPLOSS_5M(
                "additional_stoploss_5m",
                "Стоплос значение на таймфрейме 5m",
                "numeric(18, 8)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.ADDITIONAL
        ),
        TAKEPROFIT_5M(
                "additional_takeprofit_5m",
                "Тейкпрофит на таймфрейме 5m",
                "numeric(18, 8)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.ADDITIONAL
        ),
        POSITION_PRICE_5M(
                "additional_position_price_5m",
                "Цена входа в позицию на таймфрейме 5m",
                "numeric(18, 8)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.ADDITIONAL
        );

        private final ColumnMetadata metadata;

        PositionColumnType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected PositionColumn() {
        super(null);
    }

    @Inject
    public PositionColumn(CloseIndicatorM closeIndicatorM) {
        super(closeIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        return null;
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(PositionColumn.PositionColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(PositionColumn.PositionColumnType.values(), name);
    }
}
