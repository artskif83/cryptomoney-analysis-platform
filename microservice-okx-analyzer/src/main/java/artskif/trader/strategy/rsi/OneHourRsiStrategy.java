package artskif.trader.strategy.rsi;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.IndicatorFrame;
import artskif.trader.indicator.IndicatorPoint;
import artskif.trader.indicator.IndicatorSnapshot;
import artskif.trader.indicator.IndicatorType;
import artskif.trader.kafka.KafkaProducer;
import artskif.trader.strategy.AbstractStrategy;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import my.signals.v1.*;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Startup
@ApplicationScoped
public class OneHourRsiStrategy extends AbstractStrategy {

    private final static Logger LOG = Logger.getLogger(OneHourRsiStrategy.class);
    // ====== КОНСТАНТЫ RSI ======
    private static final BigDecimal RSI_30 = BigDecimal.valueOf(30);
    private static final BigDecimal RSI_40 = BigDecimal.valueOf(40);
    private static final BigDecimal RSI_50 = BigDecimal.valueOf(50);
    private static final BigDecimal RSI_60 = BigDecimal.valueOf(60);
    private static final BigDecimal RSI_70 = BigDecimal.valueOf(70);

    // ====== СОСТОЯНИЕ В ОПЕРАТИВНОЙ ПАМЯТИ ======
    private boolean canEmit = true;          // новый сигнал разрешается только после пересечения 50 на H1
    private Instant lastSignalBucket = null; // антидубль: не отдавать второй сигнал в тот же H1-бар

    @Inject
    KafkaProducer producer;
    @Inject
    protected CandleEventBus bus;
    @Inject
    protected List<IndicatorPoint> indicators; // см. AllIndicatorsProducer

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
        return "One Hour RSI Strategy";
    }

    @Override
    protected CandleEventBus getEventBus() {
        return bus;
    }

    @Override
    protected List<IndicatorPoint> getIndicators() {
        return indicators;
    }

    @Override
    protected CandleTimeframe getCandleType() {
        return CandleTimeframe.CANDLE_1H; // как и было в исходной стратегии
    }

    @Override
    public void onCandle(CandleEvent event) {
        super.onCandle(event); // соберёт IndicatorFrame и положит в lastFrame
        final var frame = getLastFrame();
        if (frame == null) return;

        // --- Достаем снапшоты RSI для 1h и 1d ---
        // Вариант A: если в IndicatorFrame лежит именно нужный ТФ
        IndicatorSnapshot rsiH1 = getRsiFromFrame(frame, IndicatorType.RSI, CandleTimeframe.CANDLE_1H);
        IndicatorSnapshot rsiD1 = getRsiFromFrame(frame, IndicatorType.RSI, CandleTimeframe.CANDLE_1D);

        // Если RSI берётся из других кадров — замени логику getRsiFromFrame на свой способ.

        // --- Текущая цена ---
        BigDecimal price = getPriceFrom(event, frame);

        Signal signal = generate(rsiH1, rsiD1, price, getStrategyKind());

//        Signal s = buildSignal(event.bucket(), BigDecimal.valueOf(10L), StrategyKind.RSI_DUAL_TF, OperationType.BUY, SignalLevel.MIDDLE);

        if (signal != null) {
            producer.sendSignal(signal);
            System.out.println("📣 SIGNAL: " + signal);
        }
    }

    public StrategyKind getStrategyKind() {
        return StrategyKind.RSI_DUAL_TF;
    }

    public Signal generate(IndicatorSnapshot rsiH1, IndicatorSnapshot rsiD1, BigDecimal price, StrategyKind kind) {
        if (rsiH1 == null || rsiD1 == null) return null;
        if (rsiH1.value() == null || rsiD1.value() == null) return null;
        if (rsiH1.lastValue() == null) return null;

        // антидубль на уровне бара H1
        Instant h1Bar = rsiH1.bucket();
        if (h1Bar != null && h1Bar.equals(lastSignalBucket)) {
            // уже отдавали сигнал на этом баре — но состояние reset всё равно обновим
            updateStateForReset(rsiH1);
            return null;
        }

        BigDecimal h1Prev = rsiH1.lastValue();
        BigDecimal h1Curr = rsiH1.value();
        BigDecimal d1Curr = rsiD1.value();

        // Правило reset: новый сигнал только после пересечения 50 на H1 в любую сторону
        if (!canEmit) {
            if (crosses(h1Prev, h1Curr, RSI_50)) {
                canEmit = true;
            }
            return null;
        }

        Signal out = null;

        // BUY: дневной RSI < 50 и H1 пересёк 30 снизу вверх
        if (d1Curr.compareTo(RSI_50) <= 0 && crossesUp(h1Prev, h1Curr, RSI_30)) {
            out = buildSignal(h1Bar != null ? h1Bar : rsiD1.bucket(), price, kind, OperationType.BUY, toLevel(d1Curr));
        }

        // SELL: дневной RSI > 60 и H1 пересёк 70 сверху вниз
        if (out == null && d1Curr.compareTo(RSI_60) >= 0 && crossesDown(h1Prev, h1Curr, RSI_70)) {
            out = buildSignal(h1Bar != null ? h1Bar : rsiD1.bucket(), price, kind, OperationType.SELL, toLevel(d1Curr));
        }

        // Запрет новых сигналов до reset’а на 50
        if (out != null) {
            canEmit = false;
            lastSignalBucket = h1Bar;
        }

        return out;
    }

    // ====== уровни по дневному RSI ======
    private static SignalLevel toLevel(BigDecimal d1) {
        if (d1.compareTo(RSI_30) < 0 || d1.compareTo(RSI_70) > 0) return SignalLevel.STRONG;
        boolean middleLow  = d1.compareTo(RSI_30) > 0 && d1.compareTo(RSI_40) < 0;
        boolean middleHigh = d1.compareTo(RSI_60) > 0 && d1.compareTo(RSI_70) < 0;
        if (middleLow || middleHigh) return SignalLevel.MIDDLE;
        if (d1.compareTo(RSI_40) > 0 && d1.compareTo(RSI_50) < 0) return SignalLevel.SMALL;

        // границы
        if (d1.compareTo(RSI_50) == 0) return SignalLevel.SMALL;
        if (d1.compareTo(RSI_40) == 0 || d1.compareTo(RSI_60) == 0) return SignalLevel.MIDDLE;
        if (d1.compareTo(RSI_30) == 0 || d1.compareTo(RSI_70) == 0) return SignalLevel.STRONG;
        return SignalLevel.LEVEL_UNSPECIFIED;
    }

    // ====== хелперы пересечений ======
    private static boolean crossesUp(BigDecimal prev, BigDecimal curr, BigDecimal level) {
        return prev.compareTo(level) < 0 && curr.compareTo(level) >= 0;
    }
    private static boolean crossesDown(BigDecimal prev, BigDecimal curr, BigDecimal level) {
        return prev.compareTo(level) > 0 && curr.compareTo(level) <= 0;
    }
    private static boolean crosses(BigDecimal prev, BigDecimal curr, BigDecimal level) {
        return (prev.compareTo(level) < 0 && curr.compareTo(level) >= 0)
                || (prev.compareTo(level) > 0 && curr.compareTo(level) <= 0);
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

    // reset-состояние на пересечении 50
    private void updateStateForReset(IndicatorSnapshot rsiH1) {
        if (rsiH1 == null || rsiH1.value() == null) return;
        BigDecimal prev = rsiH1.lastValue();
        BigDecimal curr = rsiH1.value();
        if (prev != null && !canEmit && crosses(prev, curr, RSI_50)) {
            canEmit = true;
        }
    }

    // ====== адаптеры к твоей модели данных ======
    private static IndicatorSnapshot getRsiFromFrame(IndicatorFrame frame, IndicatorType type, CandleTimeframe expectedTf) {
        return frame.getIndicator(type, expectedTf);
    }

    private static BigDecimal getPriceFrom(CandleEvent event, IndicatorFrame frame) {
        // Подставь свой источник: close-цена свечи, топик цены и т.п.
        if (event != null && event.candle() != null && event.candle().getClose() != null) {
            return event.candle().getClose();
        }
        // fallback — если цена приходит отдельным индикатором/полем кадра
        return null;
    }
}
