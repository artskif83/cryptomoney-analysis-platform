package artskif.trader.indicator;

import artskif.trader.candle.CandleType;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.events.CandleEventListener;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Startup
@ApplicationScoped
public class IndicatorFrameService  implements CandleEventListener {

    private final AtomicReference<IndicatorFrame> lastFrame = new AtomicReference<>();

    @Inject
    CandleEventBus bus;

    @Inject
    List<IndicatorPoint> indicators; // см. AllIndicatorsProducer

    @PostConstruct
    void start() {
        bus.subscribe(this); // сервис слушает ту же шину свечей
    }

    @PreDestroy
    void stop() {
        bus.unsubscribe(this);
    }

    @Override
    public void onCandle(CandleEvent ev) {
        // На каждый тик собираем значения у всех индикаторов.
        // Индикаторы сами внутри AbstractIndicator отфильтровывают типы свечей и
        // обновляют своё value (в своих потоках). Нам остаётся просто прочитать value.
        IndicatorFrame frame = assembleFrame(ev.bucket(), ev.type());
        lastFrame.set(frame);
        System.out.println("🔌 Текущий фрейм - " + frame);

        // здесь можно: логировать, отправлять дальше, класть в буфер/репозиторий и т.п.
        // System.out.println("🧩 FRAME: " + frame);
    }

    public IndicatorFrame getLastFrame() {
        return lastFrame.get();
    }

    private IndicatorFrame assembleFrame(Instant bucket, CandleType type) {
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

        return new IndicatorFrame(bucket, type, values);
    }
}
