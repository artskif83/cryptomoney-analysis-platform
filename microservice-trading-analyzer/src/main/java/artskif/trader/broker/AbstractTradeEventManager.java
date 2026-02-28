package artskif.trader.broker;

import artskif.trader.broker.client.TradingExecutorService;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.events.trade.TradeEventListener;
import artskif.trader.repository.TradeEventRepository;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor(force = true)
public abstract class AbstractTradeEventManager implements TradeEventListener {

    private static final Logger log = LoggerFactory.getLogger(AbstractTradeEventManager.class);

    private final TradeEventBus tradeEventBus;
    protected final TradeEventRepository tradeEventRepository;
    protected final BrokerConfig brokerConfig;

    // Внутренняя асинхронная шина событий
    private final BlockingQueue<Object> eventQueue = new ArrayBlockingQueue<>(1000);
    private final ExecutorService threadProcessor;
    protected final TradingExecutorService tradingExecutorService;
    private volatile boolean running = true;

    @Inject
    public AbstractTradeEventManager(TradeEventBus tradeEventBus,
                                     TradingExecutorService tradingExecutorService,
                                     TradeEventRepository tradeEventRepository,
                                     BrokerConfig brokerConfig) {
        this.tradeEventBus = tradeEventBus;
        this.tradingExecutorService = tradingExecutorService;
        this.tradeEventRepository = tradeEventRepository;
        this.brokerConfig = brokerConfig;
        this.threadProcessor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "TradeEventManager-EventProcessor");
            t.setDaemon(false);
            return t;
        });
    }

    void onStart(@Observes StartupEvent event) {
        log.info("🚀 TradeEventManager запускается...");

        // Подписываемся на события
        tradeEventBus.subscribe(this);

        // Запускаем обработчик событий в отдельном потоке
        threadProcessor.submit(this::processEvents);

        log.info("📡 TradeEventManager запущен и подписан на события");
    }

    void onShutdown(@Observes ShutdownEvent event) {
        log.info("🛑 TradeEventManager останавливается...");

        running = false;

        // Отписываемся от событий
        tradeEventBus.unsubscribe(this);

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

        log.info("🛑 TradeEventManager остановлен");
    }

    @Override
    public void onTrade(TradeEvent event) {
        // Асинхронно добавляем событие в очередь, не блокируя вызывающий поток
        if (!eventQueue.offer(event)) {
            log.warn("⚠️ Очередь событий переполнена, отбрасываем TradeEvent: {}", event);
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
     * Обработка торгового события.
     * Реализуется в дочернем классе.
     */
    protected abstract void handleTradeEvent(TradeEvent event);
}
