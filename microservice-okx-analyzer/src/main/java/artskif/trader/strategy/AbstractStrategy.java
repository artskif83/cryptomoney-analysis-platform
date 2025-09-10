package artskif.trader.strategy;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.events.CandleEventListener;
import artskif.trader.indicator.AbstractIndicator;
import artskif.trader.indicator.IndicatorFrame;
import artskif.trader.indicator.IndicatorPoint;
import artskif.trader.indicator.IndicatorSnapshot;
import artskif.trader.signal.StrategyKind;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


@NoArgsConstructor(force = true)
public abstract class AbstractStrategy implements CandleEventListener {

    protected final AtomicReference<IndicatorFrame> lastFrame = new AtomicReference<>();

    protected final CandleEventBus bus;
    protected final List<IndicatorPoint> indicators; // см. AllIndicatorsProducer

    protected AbstractStrategy(CandleEventBus bus, List<IndicatorPoint> indicators) {
        this.bus = bus;
        this.indicators = indicators;
    }

    protected abstract CandleTimeframe getCandleType();
    protected abstract StrategyKind getStrategyKind();

    @PostConstruct
    void start() {
        bus.subscribe(this); // сервис слушает ту же шину свечей
    }

    @PreDestroy
    void stop() {
        bus.unsubscribe(this);
    }

    @Override
    public void onCandle(CandleEvent event) {
        if (event.period() != getCandleType()) return;

        // На каждый тик собираем значения у всех индикаторов.
        // Индикаторы сами внутри AbstractIndicator отфильтровывают типы свечей и
        // обновляют своё value (в своих потоках). Нам остаётся просто прочитать value.
        IndicatorFrame frame = assembleFrame(event.bucket(), event.period());
        lastFrame.set(frame);
        //System.out.println("🔌 Текущий фрейм - " + frame);

        // здесь можно: логировать, отправлять дальше, класть в буфер/репозиторий и т.п.
        // System.out.println("🧩 FRAME: " + frame);
    }

    public IndicatorFrame getLastFrame() {
        return lastFrame.get();
    }

    /** Собираем полный срез по всем индикаторам */
    private IndicatorFrame assembleFrame(Instant bucket, CandleTimeframe period) {
        List<IndicatorSnapshot> snapshots = new ArrayList<>(indicators.size());

        for (IndicatorPoint ip : indicators) {
            BigDecimal value = ip.getValue();
            if (value == null) continue; // индикатор ещё не дал значение

            // Красивое имя, если индикатор наследуется от AbstractIndicator
            String name = (ip instanceof AbstractIndicator<?> ai)
                    ? ai.getName()
                    : (ip.getName() != null ? ip.getName() : ip.getClass().getSimpleName());

            IndicatorSnapshot snap = new IndicatorSnapshot(
                    name,
                    ip.getType(),
                    ip.getPeriod(),
                    ip.getCandleTimeframe(),
                    ip.getBucket(), // у конкретного индикатора bucket может отличаться, сохраняем его
                    value
            );
            snapshots.add(snap);
        }

        return new IndicatorFrame(bucket, period, snapshots);
    }
}
