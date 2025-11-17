package artskif.trader.websocket;


import artskif.trader.kafka.KafkaProducer;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.InetSocketAddress;
import java.net.Socket;

import jakarta.websocket.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ClientEndpoint
@Startup
@ApplicationScoped
public class OKXCandlesWebSocketClient {

    // === Настройки ===
    private static final String WS_ENDPOINT = "wss://ws.okx.com:8443/ws/v5/business"; // при необходимости замените на /public
    private static final String WS_HOST = "ws.okx.com";
    private static final int WS_PORT = 8443;

    private static final long RECONNECT_DELAY_MS = 5_000L;       // задержка между попытками переподключения
    private static final long WATCHDOG_PERIOD_MS = 5_000L;       // период проверки соединения
    private static final long INACTIVITY_RECONNECT_MS = 30_000L; // если нет сообщений дольше этого — переоткрываем

    private static final Logger LOG = Logger.getLogger(OKXCandlesWebSocketClient.class);

    @ConfigProperty(name = "okx.websocket.enabled", defaultValue = "true")
    boolean websocketEnabled;

    @Inject
    KafkaProducer producer;

    private volatile Session session;
    private final Map<String, BlockingQueue<String>> queues = new ConcurrentHashMap<>();
    private ExecutorService kafkaExecutor;

    // один общий планировщик — и для реконнекта, и для watchdog
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private volatile long lastActivityNanos = System.nanoTime();

    @PostConstruct
    void init() {
        if (!websocketEnabled) {
            LOG.warn("⚙️ OKX WebSocket клиент отключен (okx.websocket.enabled=false)");
            return;
        }

        queues.put("okx-candle-5m", new LinkedBlockingQueue<>(10_000));
        queues.put("okx-candle-4h", new LinkedBlockingQueue<>(10_000));
        queues.put("okx-candle-1w", new LinkedBlockingQueue<>(10_000));

        kafkaExecutor = Executors.newFixedThreadPool(queues.size());
        queues.forEach((topic, queue) ->
                kafkaExecutor.submit(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            String message = queue.take();
                            producer.sendMessage(topic, message);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        } catch (Exception ex) {
                            LOG.error("❌ Ошибка Kafka отправки: " + ex.getMessage(), ex);
                        }
                    }
                })
        );

        // запускаем watchdog
        scheduler.scheduleAtFixedRate(this::watchdog, WATCHDOG_PERIOD_MS, WATCHDOG_PERIOD_MS, TimeUnit.MILLISECONDS);

        connect();
    }

    /** Периодическая проверка «живости» соединения и трафика. */
    private void watchdog() {
        try {
            final Session s = this.session;
            if (s == null || !s.isOpen()) {
                triggerReconnect("session is null/closed");
                return;
            }
            long silenceMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastActivityNanos);
            if (silenceMs >= INACTIVITY_RECONNECT_MS) {
                LOG.info("⏳ Нет данных " + silenceMs + " ms — переоткрываем соединение");
                safeCloseAndReconnect();
            }
        } catch (Throwable t) {
            LOG.error("⚠️ Ошибка watchdog: " + t.getMessage(), t);
        }
    }

    /** Безопасно закрыть текущую сессию и запланировать переподключение. */
    private void safeCloseAndReconnect() {
        closeSessionQuietly();
        triggerReconnect("forced reopen");
    }

    /** Коалесцированный (без дубликатов) запуск переподключения. */
    private void triggerReconnect(String reason) {
        if (reconnecting.compareAndSet(false, true)) {
            LOG.info("🔁 Переподключение через 5s (" + reason + ")");
            scheduler.schedule(() -> {
                try {
                    connect();
                } finally {
                    reconnecting.set(false);
                }
            }, RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void connect() {
        closeSessionQuietly();

        // (не блокируем на этом решении) — просто для логов, чтобы понимать, есть ли вообще выход в сеть
        if (!isEndpointReachable(WS_HOST, WS_PORT, 1500)) {
            LOG.info("🌐 Хост недоступен (TCP connect не удался) — пробуем подключиться всё равно");
        }

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(WS_ENDPOINT));
            LOG.info("✅ WebSocket соединение установлено: " + WS_ENDPOINT);
            lastActivityNanos = System.nanoTime();
        } catch (Exception e) {
            LOG.error("❌ Не удалось подключиться: " + e.getMessage(), e);
            triggerReconnect("connect() failed");
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        lastActivityNanos = System.nanoTime();

        String subscribeMsg = """
        {
          "op": "subscribe",
          "args": [
            {"channel":"candle5m","instId":"BTC-USDT"},
            {"channel":"candle4H","instId":"BTC-USDT"},
            {"channel":"candle1W","instId":"BTC-USDT"}
          ]
        }""";
        session.getAsyncRemote().sendText(subscribeMsg);
        LOG.info("🔗 Подключение установлено и отправлены подписки");
    }

    @OnMessage
    public void onMessage(String message) {
        lastActivityNanos = System.nanoTime(); // фиксируем «живой» трафик
        if (LOG.isDebugEnabled()) {
            LOG.debugf("📩 Получено сообщение: %s%n", message);
        }

        String topic = determineTopic(message);
        if (topic != null) {
            BlockingQueue<String> q = queues.get(topic);
            if (q != null && !q.offer(message)) {
                LOG.warn("⚠️ Очередь заполнена, сообщение отброшено: " + topic);
            }
        }
    }

    // (опционально) если сервер/прокси присылает PONG — тоже считаем это активностью
    @OnMessage
    public void onPong(PongMessage pong) {
        ByteBuffer data = pong.getApplicationData();
        lastActivityNanos = System.nanoTime();
    }

    private String determineTopic(String message) {
        if (message.contains("\"channel\":\"candle5m\"")) return "okx-candle-5m";
        if (message.contains("\"channel\":\"candle4H\"")) return "okx-candle-4h";
        if (message.contains("\"channel\":\"candle1W\"")) return "okx-candle-1w";
        return null;
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        LOG.info("🔌 Соединение закрыто: " + reason);
        LOG.infof("🔌 Код закрытия: %s, причина: %s%n", reason.getCloseCode(), reason.getReasonPhrase());
        triggerReconnect("onClose");
    }

    @OnError
    public void onError(Session session, Throwable t) {
        LOG.error("❌ Ошибка WebSocket: " + t.getMessage());
        triggerReconnect("onError");
    }

    @PreDestroy
    public void cleanup() {
        if (!websocketEnabled) return;

        LOG.info("🧹 Завершение работы приложения...");
        if (kafkaExecutor != null) {
            kafkaExecutor.shutdown();
            try {
                if (!kafkaExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.info("⚠️ Потоки не завершились вовремя, форсируем остановку...");
                    kafkaExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                LOG.error("❌ Ожидание завершения потоков прервано");
                kafkaExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        closeSessionQuietly();
        scheduler.shutdownNow();
    }

    private void closeSessionQuietly() {
        final Session s = this.session;
        if (s != null) {
            try {
                if (s.isOpen()) s.close();
            } catch (Exception ignore) {
            } finally {
                this.session = null;
            }
        }
    }

    /** Лёгкая проверка доступности TCP-порта (для логов/диагностики). */
    private boolean isEndpointReachable(String host, int port, int timeoutMs) {
        try (Socket sock = SSLSocketFactory.getDefault().createSocket()) {
            sock.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
