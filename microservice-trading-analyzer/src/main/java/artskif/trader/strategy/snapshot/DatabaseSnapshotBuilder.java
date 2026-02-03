package artskif.trader.strategy.snapshot;

import artskif.trader.candle.AbstractCandle;
import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.entity.MetadataType;
import artskif.trader.strategy.database.ColumnsRegistry;
import artskif.trader.strategy.database.columns.Column;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.snapshot.impl.DatabaseSnapshotRow;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;

@ApplicationScoped
public class DatabaseSnapshotBuilder {

    private final ColumnsRegistry registry;
    private final Candle candle;

    @Inject
    public DatabaseSnapshotBuilder(ColumnsRegistry registry, Candle candle) {
        this.registry = registry;
        this.candle = candle;
    }

    public DatabaseSnapshot build(Bar bar, AbstractSchema schema, int barIndex, boolean isLive) {

        CandleTimeframe timeframe = schema.getTimeframe();
        AbstractCandle candleInstance = candle.getInstance(timeframe);
        Contract contract = schema.getContract();




        DatabaseSnapshotRow row = new DatabaseSnapshotRow(
                bar.getTimePeriod(),
                bar.getBeginTime(),
                schema.getContractHash()
        );

        // Добавляем базовые данные свечи
        row.addColumn("open", bar.getOpenPrice().bigDecimalValue());
        row.addColumn("high", bar.getHighPrice().bigDecimalValue());
        row.addColumn("low", bar.getLowPrice().bigDecimalValue());
        row.addColumn("close", bar.getClosePrice().bigDecimalValue());
        row.addColumn("volume", bar.getVolume().bigDecimalValue());

        for (ContractMetadata metadata : contract.metadata) {
            try {
                Column column = registry.getColumn(metadata.name).orElse(null);
                if (column != null) {
                    ColumnTypeMetadata columnTypeMetadataByValueName = column.getColumnTypeMetadataByName(metadata.name);
                    if (columnTypeMetadataByValueName != null && columnTypeMetadataByValueName.getTimeframe().equals(timeframe)) {
                        row.addColumn(metadata.name, column.getValueByName(isLive, metadata.name, barIndex).bigDecimalValue());
                    } else {
                        Log.debugf("⚠️ Колонка %s не поддерживает таймфрейм %s",
                                metadata.name, timeframe);
                    }
                } else {
                    Log.debugf("⚠️ Колонка %s не существует в реестре для колонок",
                            metadata.name);
                }
            } catch (Exception e) {
                Log.errorf(e, "❌ Ошибка при вычислении колонки %s для свечи %s",
                        metadata.name, bar.getBeginTime());
            }
        }

        return row;
    }
}
