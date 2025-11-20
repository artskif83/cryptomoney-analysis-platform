package artskif.trader.restapi;


import artskif.trader.kafka.KafkaProducer;
import artskif.trader.repository.CandleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Startup
@ApplicationScoped
public class OKXCandlesRestApiClient {

    private static final Logger LOG = Logger.getLogger(OKXCandlesRestApiClient.class);

    @Inject
    KafkaProducer producer;
    @Inject
    CandleRepository candleRepository;

    // === Config ===
    @ConfigProperty(name = "okx.history.enabled", defaultValue = "true")
    boolean historyEnabled;

    @ConfigProperty(name = "okx.history.baseUrl", defaultValue = "https://www.okx.com")
    String baseUrl;

    @ConfigProperty(name = "okx.history.instId", defaultValue = "BTC-USDT")
    String instId;

    @ConfigProperty(name = "okx.history.limit", defaultValue = "300")
    int limit;

    @ConfigProperty(name = "okx.history.startEpochMs", defaultValue = "1609459200000")
    long startEpochMs;

    @ConfigProperty(name = "okx.history.requestPauseMs", defaultValue = "250")
    long requestPauseMs;

    @ConfigProperty(name = "okx.history.maxRetries", defaultValue = "5")
    int maxRetries;

    @ConfigProperty(name = "okx.history.retryBackoffMs", defaultValue = "1000")
    long retryBackoffMs;

    @ConfigProperty(name = "okx.history.timeframes", defaultValue = "1m,4H,1W")
    List<String> timeframes;

    @ConfigProperty(name = "okx.history.pagesLimit", defaultValue = "1")
    int pagesLimit;

    @ConfigProperty(name = "okx.history.threadPoolSize", defaultValue = "3")
    int threadPoolSize;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();
    private ExecutorService executorService;

    @PostConstruct
    void onStart() {
        if (!historyEnabled) {
            LOG.warn("⚙️ OKX RestAPI Исторический клиент отключен (okx.history.enabled=false)");
            return;
        }
        LOG.infof("🚀 Старт исторического харвестера OKX: instId=%s bars=%s start=%s pagesLimit=%d limit=%d threadPoolSize=%d",
                instId, timeframes, Instant.ofEpochMilli(startEpochMs), pagesLimit, limit, threadPoolSize);

        // Создаем пул потоков для параллельной обработки таймфреймов
        executorService = Executors.newFixedThreadPool(threadPoolSize);

        try {
            // Создаем CompletableFuture для каждого таймфрейма
            List<CompletableFuture<Void>> futures = timeframes.stream()
                    .map(bar -> CompletableFuture.runAsync(() -> {
                        try {
                            harvestBar(bar);
                        } catch (Throwable t) {
                            // НЕ валим приложение — логируем и идём дальше
                            LOG.errorf(t, "❌ Невосстановимая ошибка при загрузке бара %s", bar);
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            // Ожидаем завершения всех задач
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            // Блокируем до завершения всех задач
            allFutures.join();

            LOG.info("✅ Исторический харвестер OKX завершил начальную загрузку");
        } finally {
            // Корректно останавливаем пул потоков
            shutdownExecutorService();
        }
    }

    private void shutdownExecutorService() {
        if (executorService != null && !executorService.isShutdown()) {
            LOG.info("🛑 Остановка пула потоков...");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                        LOG.error("❌ Пул потоков не остановился");
                    }
                }
            } catch (InterruptedException ie) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void harvestBar(String bar) {
        final String topic = "okx-candle-" + normalizeBarForTopic(bar) + "-history";
        LOG.infof("📥 Тянем историю: bar=%s -> topic=%s", bar, topic);

        // Получаем timestamp последней свечи из БД
        String timeframeForDb = "CANDLE_" + normalizeBarForTopic(bar).toUpperCase();
        long latestTimestamp = candleRepository.getLatestCandleTimestamp(instId, timeframeForDb, startEpochMs);

        LOG.infof("📍 Граница загрузки: bar=%s stopAt=%d (%s)",
                bar, latestTimestamp, Instant.ofEpochMilli(latestTimestamp));

        // Начинаем с текущего момента
        Long to = null;
        Long from = latestTimestamp;

        int pagesLoaded = 0;

        while (true) {
            if (pagesLoaded >= pagesLimit) {
                LOG.infof("⛳ Достигнут pagesLimit=%d для bar=%s", pagesLimit, bar);
                break;
            }

            // Используем from для загрузки данных ОТ НОВЫХ К СТАРЫМ
            Optional<JsonNode> rootOpt = callHistoryIndexCandles(instId, bar, limit, from, to);
            if (rootOpt.isEmpty()) {
                LOG.warnf("⚠️ Пропускаем страницу (исчерпаны повторы) для bar=%s to=%s", bar, 
                        to != null ? Instant.ofEpochMilli(to) : "null");
                break;
            }

            JsonNode root = rootOpt.get();
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                LOG.infof("🏁 Данных больше нет: bar=%s", bar);
                break;
            }

            // Вычислим minTs для проверки границы и пагинации
            long minTs = Long.MAX_VALUE;
            for (JsonNode arr : data) {
                long ts = arr.get(0).asLong();
                if (ts < minTs) minTs = ts;
            }

            // Выводим данные в человекочитаемом формате
            if (LOG.isDebugEnabled()) {
                LOG.debugf("📊 Полученные данные для bar=%s:", bar);
                for (JsonNode arr : data) {
                    if (arr.isArray() && arr.size() >= 6) {
                        long timestamp = arr.get(0).asLong();
                        double open = arr.get(1).asDouble();
                        double high = arr.get(2).asDouble();
                        double low = arr.get(3).asDouble();
                        double close = arr.get(4).asDouble();
                        double volume = arr.get(5).asDouble();
                        LOG.debugf("  🕐 %s | O:%.2f H:%.2f L:%.2f C:%.2f V:%.2f",
                                Instant.ofEpochMilli(timestamp), open, high, low, close, volume);
                    }
                }
            }

            // Оборачиваем данные в объект с instId
            boolean isLast = (to == null);
            String payload = String.format("{\"instId\":\"%s\",\"isLast\":%s,\"data\":%s}", instId, isLast, data);
            producer.sendMessage(topic, payload);

            // Проверяем, не достигли ли мы границы (последняя запись в БД или startEpochMs)
            if (minTs <= latestTimestamp) {
                LOG.infof("⛳ Достигнута граница загрузки: minTs=%d (%s) <= latestTimestamp=%d для bar=%s",
                        minTs, Instant.ofEpochMilli(minTs), latestTimestamp, bar);
                break;
            }

            pagesLoaded++;
            LOG.infof("📦 Отправлена страница #%d (%d записей) для bar=%s; minTs=%d (%s)",
                    pagesLoaded, data.size(), bar, minTs, Instant.ofEpochMilli(minTs));

            // Пагинация: следующий запрос должен получить данные РАНЬШЕ minTs
            to = minTs - 1;

            sleep(requestPauseMs);
        }
    }

    /**
     * Делает вызов OKX с ретраями. Возвращает Optional.empty(), если все попытки исчерпаны.
     * НЕ бросает исключение наружу — чтобы не уронить приложение и продолжить с другими барами.
     */
    private Optional<JsonNode> callHistoryIndexCandles(String instId, String bar, int limit, Long before, Long after) {
        // Тут странность OKX API потому что он выдает данные начиная от before и до after )))
        StringBuilder uri = new StringBuilder(baseUrl)
                .append("/api/v5/market/history-candles")
                .append("?instId=").append(url(instId))
                .append("&bar=").append(url(bar))
                .append("&limit=").append(limit);
        if (after != null) uri.append("&after=").append(after);
        if (before != null) uri.append("&before=").append(before);

        String fullUrl = uri.toString();
        LOG.debugf("🌐 Запрос к OKX API: %s", fullUrl);

        HttpRequest req = HttpRequest.newBuilder(URI.create(fullUrl))
                .header("Accept", "application/json")
                .GET()
                .build();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                int code = resp.statusCode();

                if (code == 200) {
                    JsonNode root = om.readTree(resp.body());
                    int s = root.path("code").asInt();
                    if (s == 0) return Optional.of(root);

                    String msg = root.path("msg").asText();
                    LOG.warnf("⚠️ OKX API error: code=%d msg=%s (attempt %d/%d)", s, msg, attempt, maxRetries);
                } else if (code == 429 || code == 418 || (code >= 500 && code < 600)) {
                    LOG.warnf("⏳ Rate/Server error HTTP %d (attempt %d/%d)", code, attempt, maxRetries);
                } else {
                    LOG.errorf("❌ HTTP %d, body=%s (attempt %d/%d)", code, resp.body(), attempt, maxRetries);
                }
            } catch (Exception e) {
                LOG.warnf("🌐 Сетевая ошибка '%s' (attempt %d/%d)", e.getMessage(), attempt, maxRetries);
            }

            long backoff = retryBackoffMs * attempt;
            sleep(backoff);
        }

        LOG.error("❌ Все попытки исчерпаны — возвращаем empty()");
        return Optional.empty();
    }

    private static String normalizeBarForTopic(String bar) {
        switch (bar) {
            case "1m":
                return "1m";
            case "4H":
                return "4h";
            case "1W":
                return "1w";
            default:
                return bar.toLowerCase(Locale.ROOT);
        }
    }

    private static String url(String s) {
        return s.replace(" ", "%20");
    }

    private static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
