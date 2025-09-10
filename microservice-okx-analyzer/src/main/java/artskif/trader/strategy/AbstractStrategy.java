package artskif.trader.strategy;

import artskif.trader.candle.CandlePeriod;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.events.CandleEventListener;
import artskif.trader.indicator.AbstractIndicator;
import artskif.trader.indicator.IndicatorFrame;
import artskif.trader.indicator.IndicatorPoint;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    protected abstract CandlePeriod getCandleType();

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
        System.out.println("🔌 Текущий фрейм - " + frame);

        // здесь можно: логировать, отправлять дальше, класть в буфер/репозиторий и т.п.
        // System.out.println("🧩 FRAME: " + frame);
    }

    public IndicatorFrame getLastFrame() {
        return lastFrame.get();
    }

    private IndicatorFrame assembleFrame(Instant bucket, CandlePeriod period) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();

        for (IndicatorPoint ip : indicators) {
            BigDecimal v = ip.getValue();
            if (v == null) continue; // индикатор ещё не дал значение

            String name;
            if (ip instanceof AbstractIndicator<?> ai) {
                name = ai.getName(); // красиво читаемое имя индикатора
            } else {
                name = ip.getClass().getSimpleName(); // fallback
            }
            values.put(name, v);
        }

        return new IndicatorFrame(bucket, period, values);
    }
}
