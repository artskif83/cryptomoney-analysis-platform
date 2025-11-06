package artskif.trader.strategy.rsi;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.IndicatorFrame;
import artskif.trader.indicator.IndicatorPoint;
import artskif.trader.indicator.IndicatorSnapshot;
import artskif.trader.kafka.KafkaProducer;
import artskif.trader.strategy.AbstractStrategy;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import my.signals.v1.*;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Startup
@ApplicationScoped
public class RsiStrategy extends AbstractStrategy {

    private final static Logger LOG = Logger.getLogger(RsiStrategy.class);

    private Instant lastSignalBucket = null; // антидубль: не отдавать второй сигнал в тот же H1-бар

    @Inject
    KafkaProducer producer;
    @Inject
    protected CandleEventBus bus;
    @Inject
    Instance<IndicatorPoint> indicators;

    @PostConstruct
    void start() {
        LOG.infof("🚀 Старт стратегии %s", getName());
        getEventBus().subscribe(this); // сервис слушает ту же шину свечей
    }

    @PreDestroy
    void stop() {
        getEventBus().unsubscribe(this);
    }

    @Override
    protected String getName() {
        return "RSI Strategy";
    }

    @Override
    protected CandleEventBus getEventBus() {
        return bus;
    }

    @Override
    protected List<IndicatorPoint> getIndicators() {
        return indicators.stream().toList();
    }

    @Override
    protected CandleTimeframe getCandleType() {
        return CandleTimeframe.CANDLE_1M;
    }

    @Override
    public void onCandle(CandleEvent event) {
        super.onCandle(event); // соберёт IndicatorFrame и положит в lastFrame
        final var frame = getLastFrame();
        if (frame == null) return;

        Signal signal = generate(frame);

//        if (signal != null) {
//            //producer.sendSignal(signal);
//            //LOG.infof("📣 SIGNAL: %s", signal);
//        }
    }

    private Signal generate(IndicatorFrame frame) {
    return buildSignal(frame.bucket(), BigDecimal.valueOf(0L), StrategyKind.RSI_DUAL_TF, OperationType.BUY, SignalLevel.SMALL);
    }

    public StrategyKind getStrategyKind() {
        return StrategyKind.RSI_DUAL_TF;
    }

    private static Signal buildSignal(Instant bucket, BigDecimal price, StrategyKind kind,
                                      OperationType op, SignalLevel lvl) {
        Signal.Builder b = Signal.newBuilder()
                .setOperation(op)
                .setStrategy(kind)
                .setLevel(lvl)
                .setId(UUID.randomUUID().toString())
                .setSymbol(Symbol.newBuilder().setBase("BTC").setQuote("USDT").build());

        if (bucket != null) {
            b.setTime(bucket);
        }
        if (price != null) {
            b.setPrice(price.doubleValue());
        }
        return b.build();
    }


}
