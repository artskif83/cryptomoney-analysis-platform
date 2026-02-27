package artskif.trader.broker;

import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventBus;
import artskif.trader.events.candle.CandleEventListener;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AbstractOrdersManager implements CandleEventListener {

    private static final Logger log = LoggerFactory.getLogger(AbstractOrdersManager.class);

    protected final CandleEventBus candleEventBus;

    // Промежуточная шина событий
    private final BlockingQueue<Object> eventQueue = new ArrayBlockingQueue<>(1000);
    private final ExecutorService threadProcessor;
    private volatile boolean running = true;

    // Требуется для CDI-прокси дочерних классов
    protected AbstractOrdersManager() {
        this.candleEventBus = null;
        this.threadProcessor = null;
    }

    @Inject
    public AbstractOrdersManager(CandleEventBus candleEventBus) {
        this.candleEventBus = candleEventBus;
        this.threadProcessor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "OrdersManager-EventProcessor");
            t.setDaemon(false);
            return t;
        });
    }

    void onStart(@Observes StartupEvent event) {
        log.info("🚀 OrdersManager запускается...");

        // Подписываемся на события
        candleEventBus.subscribe(this);

        // Запускаем обработчик событий в отдельном потоке
        threadProcessor.submit(this::processEvents);

        log.info("📡 OrdersManager запущен и подписан на события");
    }

    void onShutdown(@Observes ShutdownEvent event) {
        log.info("🛑 OrdersManager останавливается...");

        running = false;

        // Отписываемся от событий
        candleEventBus.unsubscribe(this);

        // Останавливаем обработчик
        threadProcessor.shutdown();
        try {
            if (!threadProcessor.awaitTermination(30, TimeUnit.SECONDS)) {
                threadProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadProcessor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("✅ OrdersManager остановлен");
    }

    @Override
    public void onCandle(CandleEvent event) {
        // Асинхронно добавляем событие в очередь, не блокируя вызывающий поток
        if (!eventQueue.offer(event)) {
            log.warn("⚠️ Очередь событий переполнена, отбрасываем CandleEvent: {}", event);
        }
    }

    /**
     * Основной цикл обработки событий в отдельном потоке
     */
    private void processEvents() {
        log.info("⚡ Поток обработки событий OrdersManager запущен");

        while (running) {
            try {
                // Ждем события из очереди с таймаутом
                Object event = eventQueue.poll(1, TimeUnit.SECONDS);

                if (event == null) {
                    continue;
                }

                if (event instanceof CandleEvent candleEvent) {
                    handleCandleEvent(candleEvent);
                } else {
                    log.warn("⚠️ Неизвестный тип события: {}", event.getClass());
                }

            } catch (InterruptedException e) {
                log.info("🛑 Поток обработки событий OrdersManager прерван");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("❌ Ошибка при обработке события", e);
            }
        }

        log.info("✅ Поток обработки событий OrdersManager остановлен");
    }

    /**
     * Обработка события свечи — реализуется в дочернем классе
     */
    protected abstract void handleCandleEvent(CandleEvent event);
}
