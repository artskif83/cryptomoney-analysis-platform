package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.multi.levels.CandleResistanceStrengthM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class CandleResistanceStrengthColumn extends AbstractColumn<CandleResistanceStrengthM> {

    /**
     * Перечислимый тип для различных значений CandleResistanceStrength колонки
     */
    public enum CandleResistanceStrengthColumnType implements ColumnTypeMetadata {
        RESISTANCE_5M(
                "metric_candle_resistance_strength_5m",
                "Сила сопротивления свечи на таймфрейме 5m",
                "smallint",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        INDEX_5M(
                "index_candle_5m",
                "Индекс свечи свечи на таймфрейме 5m",
                "bigint",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        INDEX_4H(
                "index_candle_4h",
                "Индекс свечи свечи на таймфрейме 4h",
                "bigint",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),
        RESISTANCE_4H(
                "metric_candle_resistance_strength_4h",
                "Сила сопротивления свечи на таймфрейме 4h",
                "smallint",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),
        RESISTANCE_5M_ON_4H(
                "metric_candle_resistance_strength_5m_on_4h",
                "Сила сопротивления свечи на таймфрейме 4h для индекса 5m",
                "smallint",
                CandleTimeframe.CANDLE_5M,
                CandleTimeframe.CANDLE_4H, // higher timeframe
                MetadataType.METRIC
        );

        private final ColumnMetadata metadata;

        CandleResistanceStrengthColumnType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected CandleResistanceStrengthColumn() {
        super(null);
    }

    @Inject
    public CandleResistanceStrengthColumn(CandleResistanceStrengthM candleResistanceStrengthM) {
        super(candleResistanceStrengthM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        ColumnTypeMetadata featureType = ColumnTypeMetadata.findByName(CandleResistanceStrengthColumnType.values(), valueName);
        switch (featureType) {
            case CandleResistanceStrengthColumnType.INDEX_5M:
                return DecimalNum.valueOf(index);
            case CandleResistanceStrengthColumnType.INDEX_4H:
                return DecimalNum.valueOf(index);
            default:
                return getValueByNameGeneric(isLiveSeries, valueName, index, CandleResistanceStrengthColumnType.values());
        }

    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(CandleResistanceStrengthColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(CandleResistanceStrengthColumnType.values(), name);
    }
}

