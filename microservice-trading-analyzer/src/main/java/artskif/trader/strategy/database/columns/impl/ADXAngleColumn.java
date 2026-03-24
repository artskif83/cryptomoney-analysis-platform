package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.multi.ADXAngleIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class ADXAngleColumn extends AbstractColumn<ADXAngleIndicatorM> {

    /**
     * Перечислимый тип для значений угла наклона ADX
     */
    public enum ADXAngleColumnType implements ColumnTypeMetadata {
        ADX_ANGLE_VALUE_1M_ON_1H(
                "metric_adx_angle_1m_on_1h",
                "Угол наклона ADX (период 14) на таймфрейме 1h для индекса 1m",
                "numeric(8, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_1H,
                MetadataType.METRIC
        );

        private final ColumnMetadata metadata;

        ADXAngleColumnType(String name, String description, String dataType,
                           CandleTimeframe timeframe, CandleTimeframe higherTimeframe,
                           MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected ADXAngleColumn() {
        super(null);
    }

    @Inject
    public ADXAngleColumn(ADXAngleIndicatorM adxAngleIndicatorM) {
        super(adxAngleIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        return getValueByNameGeneric(isLiveSeries, valueName, index, ADXAngleColumnType.values());
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(ADXAngleColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(ADXAngleColumnType.values(), name);
    }
}
