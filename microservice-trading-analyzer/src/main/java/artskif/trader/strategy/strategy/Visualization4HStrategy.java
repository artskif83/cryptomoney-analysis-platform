package artskif.trader.strategy.strategy;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.strategy.AbstractStrategy;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.database.schema.impl.TF4hSchema;
import artskif.trader.strategy.snapshot.DatabaseSnapshotBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Visualization4HStrategy extends AbstractStrategy {

    private final AbstractSchema tf4hSchema;

    public Visualization4HStrategy() {
        super(null, null, null, null, null);
        this.tf4hSchema = null;
    }

    @Inject
    protected Visualization4HStrategy(Candle candle,
                                      DatabaseSnapshotBuilder snapshotBuilder,
                                      StrategyDataService dataService,
                                      TF4hSchema tf4hSchema,
                                      TradeEventBus tradeEventBus) {
        super(candle, null, snapshotBuilder, dataService, null);
        this.tf4hSchema = tf4hSchema;
    }

    @Override
    public String getName() {
        return "TF4h Strategy";
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
