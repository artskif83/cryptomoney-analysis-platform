package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.base.ShortHighLevelIndicator;
import artskif.trader.strategy.indicators.multi.levels.ShortHighLevelIndicatorM;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class ShortHighLevelColumn extends AbstractColumn<ShortHighLevelIndicatorM> {

    /**
     * Перечислимый тип для различных значений уровня сопротивления (высший таймфрейм)
     */
    public enum ShortHighLevelColumnType implements ColumnTypeMetadata {
        SHORT_HIGH_LEVEL_1H(
                "short_high_level_1h",
                "Уровень поддержки высшего таймфрейма на таймфрейме 1h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1H,
                null,
                MetadataType.METRIC
        ),
        SHORT_HIGH_LEVEL_TOP_BORDER_1H(
                "short_high_level_top_border_1h",
                "Верхняя граница уровня поддержки на таймфрейме 1h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1H,
                null,
                MetadataType.METRIC
        ),
        SHORT_HIGH_LEVEL_BOTTOM_BORDER_1H(
                "short_high_level_bottom_border_1h",
                "Нижняя граница уровня поддержки на таймфрейме 1h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1H,
                null,
                MetadataType.METRIC
        ),
        SHORT_HIGH_LEVEL_TOP_BORDER_1M_ON_1H(
                "short_high_level_top_border_1m_on_1h",
                "Верхняя граница уровня поддержки на таймфрейме 1h для индекса 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_1H,
                MetadataType.METRIC
        ),
        SHORT_HIGH_LEVEL_BOTTOM_BORDER_1M_ON_1H(
                "short_high_level_bottom_border_1m_on_1h",
                "Нижняя граница уровня поддержки на таймфрейме 1h для индекса 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_1H,
                MetadataType.METRIC
        );

        private final ColumnMetadata metadata;

        ShortHighLevelColumnType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected ShortHighLevelColumn() {
        super(null);
    }

    @Inject
    public ShortHighLevelColumn(ShortHighLevelIndicatorM shortHighLevelIndicatorM) {
        super(shortHighLevelIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        ColumnTypeMetadata featureType = ColumnTypeMetadata.findByName(ShortHighLevelColumnType.values(), valueName);
        ColumnMetadata metadata = featureType.getMetadata();
        ShortHighLevelIndicator indicator = (ShortHighLevelIndicator) getIndicator(metadata.timeframe(), isLiveSeries);

        switch (featureType) {
            case ShortHighLevelColumnType.SHORT_HIGH_LEVEL_TOP_BORDER_1H:
                return indicator.getTopBorder(index);
            case ShortHighLevelColumnType.SHORT_HIGH_LEVEL_BOTTOM_BORDER_1H:
                return indicator.getBottomBorder(index);
            default:
                return getValueByNameGeneric(isLiveSeries, valueName, index, ShortHighLevelColumnType.values());
        }
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(ShortHighLevelColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(ShortHighLevelColumnType.values(), name);
    }
}
