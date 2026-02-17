package artskif.trader.strategy.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.strategy.AbstractStrategy;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.database.schema.impl.TF4hSchema;
import artskif.trader.strategy.event.impl.indicator.TrendDownLevel2EventProcessor;
import artskif.trader.strategy.snapshot.DatabaseSnapshotBuilder;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TF4hStrategy extends AbstractStrategy {

    private final TradeEventBus tradeEventBus;
    private final AbstractSchema tf4hSchema;

    public TF4hStrategy() {
        super(null, null, null, null);
        this.tradeEventBus = null;
        this.tf4hSchema = null;
    }

    @Inject
    protected TF4hStrategy(Candle candle,
                           TrendDownLevel2EventProcessor eventProcessor,
                           DatabaseSnapshotBuilder snapshotBuilder,
                           StrategyDataService dataService,
                           TF4hSchema tf4hSchema,
                           TradeEventBus tradeEventBus) {
        super(candle, eventProcessor, snapshotBuilder, dataService);

        this.tradeEventBus = tradeEventBus;
        this.tf4hSchema = tf4hSchema;

        // Логирование загруженного EventProcessor
        Log.infof("📦 Загружен EventProcessor: %s", eventProcessor.getClass().getSimpleName());
    }

    @Override
    public String getName() {
        return "TF4h Strategy";
    }

    @Override
    public void onBar(CandlestickDto candle) {

    }

    @Override
    protected AbstractSchema getBacktestSchema() {
        return tf4hSchema;
    }

    @Override
    protected AbstractSchema getLifetimeSchema() {
        return null;
    }

    @Override
    protected CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_4H;
    }

    @Override
    protected Integer getUnstableBars() {
        return 14;
    }
}
