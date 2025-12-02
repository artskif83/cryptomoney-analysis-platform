package artskif.trader.restapi.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * HTTP клиент с логикой повторных попыток
 */
public class RetryableHttpClient {
    private static final Logger LOG = Logger.getLogger(RetryableHttpClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    private final long retryBackoffMs;

    public RetryableHttpClient(int maxRetries, long retryBackoffMs) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.maxRetries = maxRetries;
        this.retryBackoffMs = retryBackoffMs;
    }

    /**
     * Выполнить HTTP запрос с повторными попытками
     * @param url URL для запроса
     * @return JSON ответ или empty если все попытки исчерпаны
     */
    public Optional<JsonNode> executeWithRetry(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int code = response.statusCode();

                if (code == 200) {
                    return Optional.of(objectMapper.readTree(response.body()));
                }

                if (shouldRetry(code)) {
                    LOG.warnf("⏳ HTTP %d (попытка %d/%d)", code, attempt, maxRetries);
                } else {
                    LOG.errorf("❌ HTTP %d, body=%s", code, response.body());
                    return Optional.empty();
                }
            } catch (Exception e) {
                LOG.warnf("🌐 Ошибка '%s' (попытка %d/%d)", e.getMessage(), attempt, maxRetries);
            }

            if (attempt < maxRetries) {
                sleep(retryBackoffMs * attempt);
            }
        }

        LOG.error("❌ Все попытки исчерпаны — возвращаем empty()");
        return Optional.empty();
    }

    private boolean shouldRetry(int code) {
        return code == 429 || code == 418 || (code >= 500 && code < 600);
    }

    private void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}

