package artskif.trader.strategy.strategy;

import artskif.trader.broker.BrokerConfig;
import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.events.candle.CandleEventBus;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.strategy.AbstractStrategy;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.database.schema.impl.TF1mBacktestSchema;
import artskif.trader.strategy.database.schema.impl.TF1mLifetimeSchema;
import artskif.trader.strategy.event.impl.indicator.GoldenFieldLongEventProcessor;
import artskif.trader.strategy.event.impl.indicator.GoldenFieldShortEventProcessor;
import artskif.trader.strategy.snapshot.DatabaseSnapshotBuilder;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Startup
@ApplicationScoped
public class GoldenField1MLifetimeStrategy extends AbstractStrategy {

    private final AbstractSchema tf1mBacktestSchema;
    private final AbstractSchema tf1mLifetimeSchema;

    // Конструктор без параметров для CDI proxy
    protected GoldenField1MLifetimeStrategy() {
        super(null, null, null, null,null, null, null, null);
        this.tf1mBacktestSchema = null;
        this.tf1mLifetimeSchema = null;
    }

    @Inject
    public GoldenField1MLifetimeStrategy(Candle candle,
                                         GoldenFieldShortEventProcessor shortEventProcessor,
                                         GoldenFieldLongEventProcessor longEventProcessor,
                                         DatabaseSnapshotBuilder snapshotBuilder,
                                         StrategyDataService dataService,
                                         TF1mBacktestSchema tf1mBacktestSchema,
                                         TF1mLifetimeSchema tf1mLifetimeSchema,
                                         TradeEventBus tradeEventBus,
                                         CandleEventBus candleEventBus,
                                         BrokerConfig brokerConfig) {
        super(candle, shortEventProcessor, longEventProcessor, snapshotBuilder, dataService, tradeEventBus, candleEventBus, brokerConfig);
        this.tf1mBacktestSchema = tf1mBacktestSchema;
        this.tf1mLifetimeSchema = tf1mLifetimeSchema;

    }

    @Override
    public String getName() {
        return "GoldenField1M";
    }

    @Override
    public boolean isEnabled() {
        return true;
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
