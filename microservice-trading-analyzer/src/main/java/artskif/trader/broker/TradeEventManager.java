package artskif.trader.broker;

import artskif.trader.broker.client.TradingExecutorService;
import artskif.trader.entity.TradeEventEntity;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.events.trade.TradeEventListener;
import artskif.trader.repository.TradeEventRepository;
import artskif.trader.strategy.event.common.Direction;
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
public class TradeEventManager implements TradeEventListener {

    private static final Logger log = LoggerFactory.getLogger(TradeEventManager.class);

    private final TradeEventBus tradeEventBus;
    private final TradeEventRepository tradeEventRepository;

    // Внутренняя асинхронная шина событий
    private final BlockingQueue<Object> eventQueue = new ArrayBlockingQueue<>(1000);
    private final ExecutorService threadProcessor;
    private final TradingExecutorService tradingExecutorService;
    private volatile boolean running = true;

    @Inject
    public TradeEventManager(TradeEventBus tradeEventBus,
                             TradingExecutorService tradingExecutorService,
                             TradeEventRepository tradeEventRepository) {
        this.tradeEventBus = tradeEventBus;
        this.tradingExecutorService = tradingExecutorService;
        this.tradeEventRepository = tradeEventRepository;
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

        log.info("✅ TradeEventManager остановлен");
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
     * Обработка торгового события
     */
    private void handleTradeEvent(TradeEvent event) {
        log.info("🔄 Обработка TradeEvent: {}", event);

        try {
            // Сохраняем событие в БД
            TradeEventEntity entity = new TradeEventEntity(
                    event.tradeEventData().type(),
                    event.tradeEventData().direction(),
                    event.instrument(),
                    event.tradeEventData().eventPrice(),
                    event.tradeEventData().stopLossPercentage(),
                    event.tradeEventData().takeProfitPercentage(),
                    event.tradeEventData().timeframe(),
                    event.tag(),
                    event.timestamp(),
                    event.isTest()
            );

            tradeEventRepository.save(entity);
            log.info("💾 TradeEvent успешно сохранен в БД с UUID: {}", entity.uuid);

        } catch (Exception e) {
            log.error("❌ Ошибка при сохранении TradeEvent в БД", e);
            // Продолжаем обработку даже если сохранение не удалось
        }

        // Выполняем торговые действия
        if (event.tradeEventData().direction() == Direction.SHORT) {
            log.info("📈 Получен сигнал на открытие ШОРТ позиции");
            //tradingExecutorService.placeSpotMarketSell(event.instrument(), BigDecimal.valueOf(10));
        }
    }
}
