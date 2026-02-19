package artskif.trader.strategy.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.strategy.AbstractStrategy;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.columns.ColumnTypeMetadata;
import artskif.trader.strategy.database.columns.impl.PositionColumn;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.database.schema.impl.TF1mBacktestSchema;
import artskif.trader.strategy.database.schema.impl.TF1mLifetimeSchema;
import artskif.trader.strategy.event.impl.indicator.GoldenFieldEventProcessor;
import artskif.trader.strategy.snapshot.DatabaseSnapshot;
import artskif.trader.strategy.snapshot.DatabaseSnapshotBuilder;
import artskif.trader.strategy.event.common.TradeEventData;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.Bar;
import org.ta4j.core.num.Num;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class TF1mLifetimeStrategy extends AbstractStrategy {

    private final TradeEventBus tradeEventBus;
    private final AbstractSchema tf1mBacktestSchema;
    private final AbstractSchema tf1mLifetimeSchema;


    // Конструктор без параметров для CDI proxy
    protected TF1mLifetimeStrategy() {
        super(null, null, null, null);
        this.tradeEventBus = null;
        this.tf1mBacktestSchema = null;
        this.tf1mLifetimeSchema = null;
    }

    @Inject
    public TF1mLifetimeStrategy(Candle candle,
                                GoldenFieldEventProcessor eventProcessor,
                                DatabaseSnapshotBuilder snapshotBuilder,
                                StrategyDataService dataService,
                                TF1mBacktestSchema tf1mBacktestSchema,
                                TF1mLifetimeSchema tf1mLifetimeSchema,
                                TradeEventBus tradeEventBus) {
        super(candle, eventProcessor, snapshotBuilder, dataService);
        this.tradeEventBus = tradeEventBus;
        this.tf1mBacktestSchema = tf1mBacktestSchema;
        this.tf1mLifetimeSchema = tf1mLifetimeSchema;

    }

    @Override
    public String getName() {
        return "TF1m Strategy";
    }

    @Override
    public void onBar(CandlestickDto candle) {
        if (lifetimeBarSeries == null) {
            Log.warn("⏳ Серия баров еще не инициализирована, пропускаем обработку");
            return;
        }

        Map<ColumnTypeMetadata, Num> additionalColumns = new HashMap<>();
        int endIndex = lifetimeBarSeries.getEndIndex();
        Bar bar = lifetimeBarSeries.getBar(endIndex);

        if (candle.getTimestamp() != bar.getBeginTime()) {
            Log.warnf(
                    "⏳ Полученный бар с timestamp %s не совпадает с последним баром серии с timestamp %s, пропускаем обработку",
                    candle.getTimestamp(),
                    bar.getBeginTime()
            );
            return;
        }

        DatabaseSnapshot dbRow = snapshotBuilder.build(bar, getName()+"-lifetime", getBacktestSchema(), additionalColumns, endIndex, true);
        // Сохраняем в БД
        dataService.insertFeatureRow(dbRow);

        Optional<TradeEventData> tradeEvent = tradeEventProcessor.checkLifeTradeEvent(endIndex);

        if (tradeEvent.isPresent()) {
            TradeEventData eventData = tradeEvent.get();
            Log.infof(
                    "✅ Произошло торговое событие: %s %s (%s) [Режим: %s, Процессор: %s]",
                    eventData.type(),
                    eventData.direction(),
                    tradeEventProcessor.getClass().getSimpleName()
            );

            // Публикуем событие TradeEvent
            tradeEventBus.publish(new TradeEvent(
                    eventData,
                    candle.getInstrument(),
                    candle.getTimestamp(),
                    false
            ));
        }
    }

    @Override
    protected CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_1M;
    }

    @Override
    protected Integer getUnstableBars() {
        return 14;
    }

    @Override
    protected AbstractSchema getBacktestSchema() {
        return tf1mBacktestSchema;
    }

    @Override
    protected AbstractSchema getLifetimeSchema() {
        return tf1mLifetimeSchema;
    }
}
