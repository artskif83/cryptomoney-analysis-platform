package artskif.trader.indicator.adx;


import artskif.trader.candle.Candle1m;
import artskif.trader.candle.CandleType;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.events.CandleEventListener;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Startup
@ApplicationScoped
public class AdxIndicator1m extends AbstractAdxIndicator implements CandleEventListener, Runnable {

    @Inject
    AdxRepository adxBufferRepository;
    @Inject
    Candle1m candle1m;
    @Inject
    CandleEventBus bus;

    private final BlockingQueue<CandleEvent> queue = new ArrayBlockingQueue<>(4096, true);
    private final AdxBuffer buffer = new AdxBuffer(Duration.ofMinutes(1), 100);
    private final AdxCalculator calculator = new AdxCalculator();
    private final Path pathForSave = Paths.get("adxIndicator1m.json");

    private Thread worker;
    private volatile boolean running = false;

    @PostConstruct
    void init() {
        System.out.println("🔌 [" + getName() + "] Запуск процесса подсчета ADX индикатора");

        restoreBuffer();
        // подписка на события и старт фонового потока
        bus.subscribe(this);
        running = true;
        worker = new Thread(this, getName() + "-worker");
        worker.start();
    }

    @PreDestroy
    void shutdown() {
        bus.unsubscribe(this);
        running = false;
        if (worker != null) worker.interrupt();
    }

    @Override
    public void onCandle(CandleEvent event) {
        if (event.type() != CandleType.CANDLE_1M) return;

        // Не блокируем продьюсера: если переполнено — логируем дроп
        // При желании можно заменить на offer(ev, timeout, unit) или политику "drop oldest".
        boolean offered = queue.offer(event);
        if (!offered) {
            System.err.println("❌ [" + getName() + "] Очередь обработки переполнена, событие отброшено: " + event);
        }
    }

    @Override
    public void run() {
        System.out.println("🔗 [" + getName() + "] Запущен поток подсчета ADX индикатора: " + Thread.currentThread().getName());
        while (running) {
            try {
                process(queue.take());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                System.out.println("❌ [" + getName() + "] Не удалось обработать свечу в потоке: " + Thread.currentThread().getName() + " ошибка - " + ignored);
            }
        }
    }

    private void process(CandleEvent ev) {
        CandlestickDto c = ev.candle();
        Instant bucket = ev.bucket();
        Map<Instant, CandlestickDto> history = candle1m.getBuffer().getSnapshot();
        Optional<AdxPoint> point = AdxCalculator.computeLastAdx(history, true);
        point.ifPresent(p -> buffer.putItem(bucket, p));

        System.out.println("📥 [" + getName() + "] Свеча - " + c);
        System.out.println("📥 [" + getName() + "] Получено новое значение  ADX - " + point.orElse(null));

        // коммитим новое состояние ТОЛЬКО если свеча подтверждена (внутри calc уже учтено)
        if (Boolean.TRUE.equals(c.getConfirmed())) {
            saveBuffer();
        }
    }

    @Override
    public AdxBuffer getBuffer() {
        return buffer;
    }

    @Override
    public String getName() {
        return "1m-ADX";
    }

    @Override
    public Path getPathForSave() {
        return pathForSave;
    }

    @Override
    public AdxRepository getBufferRepository() {
        return adxBufferRepository;
    }
}
