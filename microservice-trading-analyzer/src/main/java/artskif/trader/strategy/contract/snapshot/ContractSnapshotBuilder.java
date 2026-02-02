package artskif.trader.strategy.contract.snapshot;

import artskif.trader.candle.AbstractCandle;
import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleInstance;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.contract.ContractRegistry;
import artskif.trader.strategy.contract.features.Feature;
import artskif.trader.strategy.contract.features.FeatureTypeMetadata;
import artskif.trader.strategy.contract.labels.Label;
import artskif.trader.strategy.contract.schema.AbstractSchema;
import artskif.trader.strategy.contract.snapshot.impl.ContractSnapshotRow;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ContractSnapshotBuilder {

    private final ContractRegistry registry;
    private final Candle candle;

    @Inject
    public ContractSnapshotBuilder(ContractRegistry registry, Candle candle) {
        this.registry = registry;
        this.candle = candle;
    }

    public ContractSnapshot build(AbstractSchema schema, int barIndex, boolean isLive) {

        CandleTimeframe timeframe = schema.getTimeframe();
        AbstractCandle candleInstance = candle.getInstance(timeframe);
        Contract contract = schema.getContract();

        BarSeries barSeries;
        if (isLive) {
            barSeries = candleInstance.getLiveBarSeries();
        } else {
            barSeries = candleInstance.getHistoricalBarSeries();
        }

        Bar bar = barSeries.getBar(barIndex);


        ContractSnapshotRow row = new ContractSnapshotRow(
                bar.getTimePeriod(),
                bar.getBeginTime(),
                schema.getContractHash()
        );

        // Добавляем базовые данные свечи
        row.addFeature("open", bar.getOpenPrice().bigDecimalValue());
        row.addFeature("high", bar.getHighPrice().bigDecimalValue());
        row.addFeature("low", bar.getLowPrice().bigDecimalValue());
        row.addFeature("close", bar.getClosePrice().bigDecimalValue());
        row.addFeature("volume", bar.getVolume().bigDecimalValue());

        for (ContractMetadata metadata : contract.metadata) {
            try {

                // Вычисляем значение фичи
                if (metadata.metadataType == MetadataType.FEATURE) {
                    Feature feature = registry.getFeature(metadata.name).orElse(null);
                    if (feature != null) {
                        FeatureTypeMetadata featureTypeMetadataByValueName = feature.getFeatureTypeMetadataByValueName(metadata.name);
                        if (featureTypeMetadataByValueName != null && featureTypeMetadataByValueName.getTimeframe().equals(timeframe)) {
                            row.addFeature(metadata.name, feature.getValueByName(isLive, metadata.name, barIndex).bigDecimalValue());
                        } else {
                            Log.debugf("⚠️ Фича %s не поддерживает таймфрейм %s",
                                    metadata.name, timeframe);
                        }
                    } else {
                        Log.debugf("⚠️ Фича %s не существует в реестре для фич",
                                metadata.name);
                    }
                } else if (metadata.metadataType == MetadataType.LABEL) {
                    Label label = registry.getLabel(metadata.name).orElse(null);

                    if (label != null) {
                        BigDecimal value = label.getValue(timeframe, barIndex);
                        row.addFeature(metadata.name, value != null ? value.intValue() : null);
                    } else {
                        Log.debugf("⚠️ Лейбл %s не существует в реестре для лейблов",
                                metadata.name);
                    }
                }

            } catch (Exception e) {
                Log.errorf(e, "❌ Ошибка при вычислении фичи/лейбла %s для свечи %s",
                        metadata.name, bar.getBeginTime());
            }
        }

        return row;
    }
}
