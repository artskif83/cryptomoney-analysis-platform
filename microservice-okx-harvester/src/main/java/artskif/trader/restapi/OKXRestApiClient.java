package artskif.trader.restapi;


import artskif.trader.kafka.KafkaProducer;
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
import java.util.concurrent.TimeUnit;

@Startup
@ApplicationScoped
public class OKXRestApiClient {

    private static final Logger LOG = Logger.getLogger(OKXRestApiClient.class);

    @Inject KafkaProducer producer;

    // === Config ===
    @ConfigProperty(name = "okx.history.enabled", defaultValue = "true")
    boolean historyEnabled;

    @ConfigProperty(name = "okx.history.base-url", defaultValue = "https://www.okx.com")
    String baseUrl;

    @ConfigProperty(name = "okx.history.inst-id", defaultValue = "BTC-USDT")
    String instId;

    @ConfigProperty(name = "okx.history.limit", defaultValue = "100")
    int limit;

    @ConfigProperty(name = "okx.history.start-epoch-ms", defaultValue = "1609459200000")
    long startEpochMs;

    @ConfigProperty(name = "okx.history.request-pause-ms", defaultValue = "250")
    long requestPauseMs;

    @ConfigProperty(name = "okx.history.max-retries", defaultValue = "5")
    int maxRetries;

    @ConfigProperty(name = "okx.history.retry-backoff-ms", defaultValue = "1000")
    long retryBackoffMs;

    @ConfigProperty(name = "okx.history.bars", defaultValue = "1m,1H,4H,1D")
    List<String> bars;

    @ConfigProperty(name = "okx.history.pages-limit", defaultValue = "50")
    int pagesLimit;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    @PostConstruct
    void onStart() {
        if (!historyEnabled) {
            LOG.warn("⚙️ OKX RestAPI Исторический клиент отключен (okx.history.enabled=false)");
            return;
        }
        LOG.infof("🚀 Старт исторического харвестера OKX: instId=%s bars=%s start=%s pagesLimit=%d limit=%d",
                instId, bars, Instant.ofEpochMilli(startEpochMs), pagesLimit, limit);

        for (String bar : bars) {
            try {
                harvestBar(bar);
            } catch (Throwable t) {
                // НЕ валим приложение — логируем и идём дальше к следующему бару
                LOG.errorf(t, "❌ Невосстановимая ошибка при загрузке бара %s, продолжаем со следующими", bar);
            }
        }
        LOG.info("✅ Исторический харвестер OKX завершил начальную загрузку");
    }

    private void harvestBar(String bar) {
        final String topic = "okx-candle-" + normalizeBarForTopic(bar) + "-history";
        LOG.infof("📥 Тянем историю: bar=%s -> topic=%s", bar, topic);

        Long before = null; // первый запрос — без before
        int pagesLoaded = 0;

        while (true) {
            if (pagesLoaded >= pagesLimit) {
                LOG.infof("⛳ Достигнут pagesLimit=%d для bar=%s", pagesLimit, bar);
                break;
            }

            Optional<JsonNode> rootOpt = callHistoryIndexCandles(instId, bar, limit, null, before);
            if (rootOpt.isEmpty()) {
                LOG.warnf("⚠️ Пропускаем страницу (исчерпаны повторы) для bar=%s before=%s", bar, String.valueOf(before));
                // идём к следующему бару
                break;
            }

            JsonNode root = rootOpt.get();
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                LOG.infof("🏁 Данных больше нет: bar=%s", bar);
                break;
            }

            // Вычислим minTs для условия остановки по времени
            long minTs = Long.MAX_VALUE;
            for (JsonNode arr : data) {
                // массив формата [ts, o, h, l, c, ...] — берём только ts
                long ts = arr.get(0).asLong();
                if (ts < minTs) minTs = ts;
            }

            // Отправляем ПАЧКУ целиком «как есть» — JSON массива data
            String payload = data.toString(); // одно сообщение = вся страница
            producer.sendMessage(topic, payload);

            pagesLoaded++;
            LOG.infof("📦 Отправлена страница #%d (%d записей) для bar=%s; minTs=%d",
                    pagesLoaded, data.size(), bar, minTs);

            // пагинация назад по before
            before = minTs - 1;

            // условие выхода по нижней границе
            if (before < startEpochMs) {
                LOG.infof("⛳ Достигнута нижняя граница startEpochMs=%d для bar=%s", startEpochMs, bar);
                break;
            }

            sleep(requestPauseMs);
        }
    }

    /**
     * Делает вызов OKX с ретраями. Возвращает Optional.empty(), если все попытки исчерпаны.
     * НЕ бросает исключение наружу — чтобы не уронить приложение и продолжить с другими барами.
     */
    private Optional<JsonNode> callHistoryIndexCandles(String instId, String bar, int limit, Long after, Long before) {
        StringBuilder uri = new StringBuilder(baseUrl)
                .append("/api/v5/market/history-index-candles")
                .append("?instId=").append(url(instId))
                .append("&bar=").append(url(bar))
                .append("&limit=").append(limit);
        if (after != null)  uri.append("&after=").append(after);
        if (before != null) uri.append("&before=").append(before);

        HttpRequest req = HttpRequest.newBuilder(URI.create(uri.toString()))
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
            case "1m": return "1m";
            case "1H": return "1h";
            case "4H": return "4h";
            case "1D": return "1d";
            default:   return bar.toLowerCase(Locale.ROOT);
        }
    }

    private static String url(String s) {
        return s.replace(" ", "%20");
    }

    private static void sleep(long ms) {
        try { TimeUnit.MILLISECONDS.sleep(ms); } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
