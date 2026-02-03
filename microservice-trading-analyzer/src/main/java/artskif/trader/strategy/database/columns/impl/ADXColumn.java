package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.indicators.multi.ADXIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class ADXColumn extends AbstractColumn<ADXIndicatorM> {

    public static final int ADX_PERIOD = 14;

    /**
     * Перечислимый тип для различных значений ADX фичи
     */
    public enum ADXColumnType implements ColumnTypeMetadata {
        ADX_5M(
                "feature_adx_14_5m",
                "ADX индикатор с периодом 14 на таймфрейме 5m",
                "numeric(5, 2)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.FEATURE
        ),
        ADX_4H(
                "feature_adx_14_4h",
                "ADX индикатор с периодом 14 на таймфрейме 4h",
                "numeric(5, 2)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.FEATURE
        ),
        ADX_5M_ON_4H(
                "feature_adx_14_5m_on_4h",
                "ADX индикатор с периодом 14 на таймфрейме 4h для индекса 5m",
                "numeric(5, 2)",
                CandleTimeframe.CANDLE_5M,
                CandleTimeframe.CANDLE_4H, // higher timeframe
                MetadataType.FEATURE
        );

        private final ColumnMetadata metadata;

        ADXColumnType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }


    // No-args constructor required by CDI
    protected ADXColumn() {
        super(null);
    }

    @Inject
    public ADXColumn(ADXIndicatorM adxIndicatorM) {
        super(adxIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        return getValueByNameGeneric(isLiveSeries, valueName, index, ADXColumnType.values());
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(ADXColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(ADXColumnType.values(), name);
    }
}
