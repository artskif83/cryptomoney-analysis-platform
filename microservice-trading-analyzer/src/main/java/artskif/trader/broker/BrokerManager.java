package artskif.trader.broker;

import artskif.trader.broker.client.TradingExecutorService;
import artskif.trader.events.trade.TradeEvent;
import artskif.trader.events.trade.TradeEventBus;
import artskif.trader.events.trade.TradeEventListener;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventType;
import artskif.trader.strategy.regime.common.MarketRegime;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Startup
@ApplicationScoped
public class BrokerManager implements TradeEventListener {

    private static final Logger log = LoggerFactory.getLogger(BrokerManager.class);

    private final TradeEventBus tradeEventBus;

    // Внутренняя асинхронная шина событий
    private final BlockingQueue<Object> eventQueue = new ArrayBlockingQueue<>(1000);
    private final ExecutorService eventProcessor;
    private final TradingExecutorService tradingExecutorService;
    private volatile boolean running = true;

    @Inject
    public BrokerManager(TradeEventBus tradeEventBus, TradingExecutorService tradingExecutorService) {
        this.tradeEventBus = tradeEventBus;
        this.tradingExecutorService = tradingExecutorService;
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

        // Запускаем обработчик событий в отдельном потоке
        eventProcessor.submit(this::processEvents);

        log.info("📡 PositionManager запущен и подписан на события");
    }

    void onShutdown(@Observes ShutdownEvent event) {
        log.info("🛑 PositionManager останавливается...");

        running = false;

        // Отписываемся от событий
        tradeEventBus.unsubscribe(this);

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

        if (event.type() == TradeEventType.WATERFALL && event.direction() == Direction.SHORT) {
            log.info("📈 Получен сигнал на открытие ШОРТ позиции");
            tradingExecutorService.openShort(event.instrument(), BigDecimal.valueOf(10));
        }
    }
}
