package artskif.trader.strategy.database.columns.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.ColumnMetadata;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.indicators.base.LongHighLevelIndicator;
import artskif.trader.strategy.indicators.multi.levels.LongHighLevelIndicatorM;
import artskif.trader.strategy.indicators.util.IndicatorUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.num.Num;

import java.util.List;

@ApplicationScoped
public class LongHighLevelColumn extends AbstractColumn<LongHighLevelIndicatorM> {

    /**
     * Перечислимый тип для различных значений уровня сопротивления (высший таймфрейм) для лонга
     */
    public enum LongHighLevelColumnType implements ColumnTypeMetadata {
        LONG_HIGH_LEVEL_1H(
                "long_high_level_1h",
                "Уровень сопротивления высшего таймфрейма для лонга на таймфрейме 1h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1H,
                null,
                MetadataType.METRIC
        ),
        LONG_HIGH_LEVEL_TOP_BORDER_1H(
                "long_high_level_top_border_1h",
                "Верхняя граница уровня сопротивления для лонга на таймфрейме 1h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1H,
                null,
                MetadataType.METRIC
        ),
        LONG_HIGH_LEVEL_BOTTOM_BORDER_1H(
                "long_high_level_bottom_border_1h",
                "Нижняя граница уровня сопротивления для лонга на таймфрейме 1h",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1H,
                null,
                MetadataType.METRIC
        ),
        LONG_HIGH_LEVEL_TOP_BORDER_1M_ON_1H(
                "long_high_level_top_border_1m_on_1h",
                "Верхняя граница уровня сопротивления для лонга на таймфрейме 1h для индекса 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_1H,
                MetadataType.METRIC
        ),
        LONG_HIGH_LEVEL_BOTTOM_BORDER_1M_ON_1H(
                "long_high_level_bottom_border_1m_on_1h",
                "Нижняя граница уровня сопротивления для лонга на таймфрейме 1h для индекса 1m",
                "numeric(12, 4)",
                CandleTimeframe.CANDLE_1M,
                CandleTimeframe.CANDLE_1H,
                MetadataType.METRIC
        );

        private final ColumnMetadata metadata;

        LongHighLevelColumnType(String name, String description, String dataType, CandleTimeframe timeframe, CandleTimeframe higherTimeframe, MetadataType metadataType) {
            this.metadata = new ColumnMetadata(name, description, dataType, timeframe, higherTimeframe, metadataType);
        }

        @Override
        public ColumnMetadata getMetadata() {
            return metadata;
        }
    }

    // No-args constructor required by CDI
    protected LongHighLevelColumn() {
        super(null);
    }

    @Inject
    public LongHighLevelColumn(LongHighLevelIndicatorM longHighLevelIndicatorM) {
        super(longHighLevelIndicatorM);
    }

    @Override
    public Num getValueByName(boolean isLiveSeries, String valueName, int index) {
        ColumnTypeMetadata featureType = ColumnTypeMetadata.findByName(LongHighLevelColumnType.values(), valueName);
        ColumnMetadata metadata = featureType.getMetadata();
        LongHighLevelIndicator indicator = (LongHighLevelIndicator) getIndicator(metadata.timeframe(), isLiveSeries);
        LongHighLevelIndicator higherTimeframeIndicator = (LongHighLevelIndicator) getIndicator(metadata.higherTimeframe(), isLiveSeries);
        int higherTfIndex;

        switch (featureType) {
            case LongHighLevelColumnType.LONG_HIGH_LEVEL_TOP_BORDER_1H:
                return indicator.getTopBorder(index);
            case LongHighLevelColumnType.LONG_HIGH_LEVEL_BOTTOM_BORDER_1H:
                return indicator.getBottomBorder(index);
            case LongHighLevelColumnType.LONG_HIGH_LEVEL_BOTTOM_BORDER_1M_ON_1H:
                higherTfIndex = IndicatorUtils.mapToHigherTfIndex(indicator.getBarSeries().getBar(index), higherTimeframeIndicator.getBarSeries());
                return higherTimeframeIndicator.getBottomBorder(higherTfIndex);
            case LongHighLevelColumnType.LONG_HIGH_LEVEL_TOP_BORDER_1M_ON_1H:
                higherTfIndex = IndicatorUtils.mapToHigherTfIndex(indicator.getBarSeries().getBar(index), higherTimeframeIndicator.getBarSeries());
                return higherTimeframeIndicator.getTopBorder(higherTfIndex);
            default:
                return getValueByNameGeneric(isLiveSeries, valueName, index, LongHighLevelColumnType.values());
        }
    }

    @Override
    public List<String> getColumnNames() {
        return ColumnTypeMetadata.getNames(LongHighLevelColumnType.values());
    }

    @Override
    public ColumnTypeMetadata getColumnTypeMetadataByName(String name) {
        return ColumnTypeMetadata.findByName(LongHighLevelColumnType.values(), name);
    }
}
