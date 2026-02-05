package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.indicators.multi.RSIIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class RSIColumn extends AbstractColumn<RSIIndicatorM> {

    /**
     * Перечислимый тип для различных значений RSI фичи
     */
    public enum RSIColumnType implements ColumnTypeMetadata {
        RSI_5M(
                "metric_rsi_14_5m",
                "RSI индикатор с периодом 14 на таймфрейме 5m",
                "numeric(5, 2)",
                CandleTimeframe.CANDLE_5M,
                null,
                MetadataType.METRIC
        ),
        RSI_4H(
                "metric_rsi_14_4h",
                "RSI индикатор с периодом 14 на таймфрейме 4h",
                "numeric(5, 2)",
                CandleTimeframe.CANDLE_4H,
                null,
                MetadataType.METRIC
        ),
        RSI_5M_ON_4H(
                "metric_rsi_14_5m_on_4h",
                "RSI индикатор с периодом 14 на таймфрейме 4h для индекса 5m",
                "numeric(5, 2)",
                CandleTimeframe.CANDLE_5M,
                CandleTimeframe.CANDLE_4H, // higher timeframe
                MetadataType.METRIC
        );

        private final ColumnMetadata metadata;

        RSIColumnType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected RSIColumn() {
        super(null);
    }

    @Inject
    public RSIColumn(RSIIndicatorM rsiIndicatorM) {
        super(rsiIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        return getValueByNameGeneric(isLiveSeries, valueName, index, RSIColumnType.values());
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(RSIColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(RSIColumnType.values(), name);
    }
}

