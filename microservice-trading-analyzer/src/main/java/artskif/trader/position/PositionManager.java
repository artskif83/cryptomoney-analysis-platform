package artskif.trader.position;

import artskif.trader.events.regime.RegimeChangeEvent;
import artskif.trader.events.regime.RegimeChangeEventBus;
import artskif.trader.events.regime.RegimeChangeEventListener;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.events.trade.TradeEventListener;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Startup
@ApplicationScoped
public class PositionManager implements TradeEventListener, RegimeChangeEventListener {

    private static final Logger log = LoggerFactory.getLogger(PositionManager.class);

    private final TradeEventBus tradeEventBus;
    private final RegimeChangeEventBus regimeChangeEventBus;

    // Внутренняя асинхронная шина событий
    private final BlockingQueue<Object> eventQueue = new ArrayBlockingQueue<>(1000);
    private final ExecutorService eventProcessor;
    private volatile boolean running = true;

    @Inject
    public PositionManager(TradeEventBus tradeEventBus, RegimeChangeEventBus regimeChangeEventBus) {
        this.tradeEventBus = tradeEventBus;
        this.regimeChangeEventBus = regimeChangeEventBus;
        this.eventProcessor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PositionManager-EventProcessor");
            t.setDaemon(false);
            return t;
        });
    }

    void onStart(@Observes StartupEvent event) {
        log.info("🚀 PositionManager запускается...");

        // Подписываемся на события
        tradeEventBus.subscribe(this);
        regimeChangeEventBus.subscribe(this);

        // Запускаем обработчик событий в отдельном потоке
        eventProcessor.submit(this::processEvents);

        log.info("📡 PositionManager запущен и подписан на события");
    }

    void onShutdown(@Observes ShutdownEvent event) {
        log.info("🛑 PositionManager останавливается...");

        running = false;

        // Отписываемся от событий
        tradeEventBus.unsubscribe(this);
        regimeChangeEventBus.unsubscribe(this);

        // Останавливаем обработчик
        eventProcessor.shutdown();
        try {
            if (!eventProcessor.awaitTermination(30, TimeUnit.SECONDS)) {
                eventProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            eventProcessor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("✅ PositionManager остановлен");
    }

    @Override
    public void onTrade(TradeEvent event) {
        // Асинхронно добавляем событие в очередь, не блокируя вызывающий поток
        if (!eventQueue.offer(event)) {
            log.warn("⚠️ Очередь событий переполнена, отбрасываем TradeEvent: {}", event);
        }
    }

    @Override
    public void onRegimeChange(RegimeChangeEvent event) {
        // Асинхронно добавляем событие в очередь, не блокируя вызывающий поток
        if (!eventQueue.offer(event)) {
            log.warn("⚠️ Очередь событий переполнена, отбрасываем RegimeChangeEvent: {}", event);
        }
    }

    /**
     * Основной цикл обработки событий в отдельном потоке
     */
    private void processEvents() {
        log.info("⚡ Поток обработки событий запущен");

        while (running) {
            try {
                // Ждем события из очереди с таймаутом
                Object event = eventQueue.poll(1, TimeUnit.SECONDS);

                if (event == null) {
                    continue;
                }

                if (event instanceof TradeEvent tradeEvent) {
                    handleTradeEvent(tradeEvent);
                } else if (event instanceof RegimeChangeEvent regimeChangeEvent) {
                    handleRegimeChangeEvent(regimeChangeEvent);
                } else {
                    log.warn("⚠️ Неизвестный тип события: {}", event.getClass());
                }

            } catch (InterruptedException e) {
                log.info("🛑 Поток обработки событий прерван");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("❌ Ошибка при обработке события", e);
            }
        }

        log.info("✅ Поток обработки событий остановлен");
    }

    /**
     * Обработка торгового события
     */
    private void handleTradeEvent(TradeEvent event) {
        log.info("🔄 Обработка TradeEvent: {}", event);

        // TODO: Реализация обработки торгового сигнала:
        // 1. Проверка риск-менеджмента
        // 2. Проверка текущих позиций
        // 3. Проверка состояния счета
        // 4. Принятие решения об открытии/закрытии позиции
        // 5. Отправка ордера в microservice-executor
    }

    /**
     * Обработка события смены режима рынка
     */
    private void handleRegimeChangeEvent(RegimeChangeEvent event) {
        log.info("🔄 Обработка RegimeChangeEvent: {}", event);

        // TODO: Реализация обработки смены режима:
        // 1. Анализ влияния на текущие позиции
        // 2. Корректировка стратегии риск-менеджмента
        // 3. Возможное закрытие позиций при неблагоприятном режиме
        // 4. Обновление параметров торговли
    }
}
