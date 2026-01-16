package artskif.trader.executor.market.okx;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Базовый клиент для работы с OKX REST API.
 * Содержит общие элементы для аутентификации и выполнения запросов.
 */
public class OkxApiClient {

    private static final Logger log = LoggerFactory.getLogger(OkxApiClient.class);

    protected static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    protected final ObjectMapper mapper = new ObjectMapper();

    protected final String apiKey;
    protected final String apiSecret;
    protected final String passphrase;
    protected final String restApiUrl;
    protected final OkHttpClient http;

    /**
     * Конструктор для использования в Spring-контексте
     */
    public OkxApiClient(String restApiUrl, String apiKey, String apiSecret, String passphrase) {
        this.restApiUrl = restApiUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.passphrase = passphrase;
        this.http = createDefaultHttpClient();
    }

    /**
     * Конструктор для тестов с кастомным HTTP-клиентом
     */
    public OkxApiClient(String restApiUrl, String apiKey, String apiSecret, String passphrase, OkHttpClient httpClient) {
        this.restApiUrl = restApiUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.passphrase = passphrase;
        this.http = httpClient;
    }

    /**
     * Создает HTTP-клиент с настройками по умолчанию
     */
    protected OkHttpClient createDefaultHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Выполняет аутентифицированный REST-запрос к OKX API
     *
     * @param method   HTTP метод (GET, POST, и т.д.)
     * @param endpoint API эндпоинт (например, /api/v5/trade/order)
     * @param body     Тело запроса (может быть null для GET-запросов)
     * @return Распарсенный JSON-ответ в виде Map
     * @throws IOException в случае ошибки сети или HTTP
     */
    protected Map<String, Object> executeRestRequest(String method, String endpoint, String body) throws IOException {
        String timestamp = getIsoTimestamp();
        String bodyForSign = (body != null && !body.isEmpty()) ? body : "";
        String sign = generateSignature(timestamp, method, endpoint, bodyForSign);

        Request.Builder requestBuilder = new Request.Builder()
                .url(restApiUrl + endpoint)
                .addHeader("OK-ACCESS-KEY", apiKey)
                .addHeader("OK-ACCESS-SIGN", sign)
                .addHeader("OK-ACCESS-TIMESTAMP", timestamp)
                .addHeader("OK-ACCESS-PASSPHRASE", passphrase)
                .addHeader("Content-Type", "application/json");

        if ("POST".equals(method)) {
            requestBuilder.post(RequestBody.create(bodyForSign, JSON));
        } else if ("GET".equals(method)) {
            requestBuilder.get();
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        Request request = requestBuilder.build();

        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";

            if (!response.isSuccessful()) {
                throw new IOException("HTTP запрос завершился с ошибкой: " + response.code() + " " + response.message() + ", тело: " + responseBody);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(responseBody, Map.class);
            return result;
        }
    }

    /**
     * Генерирует подпись для REST API согласно требованиям OKX
     *
     * @param timestamp Временная метка ISO 8601
     * @param method    HTTP метод (в верхнем регистре)
     * @param endpoint  API эндпоинт
     * @param body      Тело запроса
     * @return Base64-закодированная подпись HMAC SHA256
     */
    protected String generateSignature(String timestamp, String method, String endpoint, String body) {
        try {
            // OKX требует: timestamp + method.toUpperCase() + endpoint + body
            String prehash = timestamp + method.toUpperCase() + endpoint + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(prehash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("❌ Не удалось сгенерировать подпись: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось сгенерировать подпись", e);
        }
    }

    /**
     * Получает текущую временную метку в формате ISO 8601, требуемом OKX
     *
     * @return Строка формата yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     */
    protected String getIsoTimestamp() {
        // OKX требует формат ISO 8601: 2024-01-16T10:30:45.123Z (только миллисекунды!)
        // Instant.toString() может давать микросекунды/наносекунды, что OKX не понимает
        // Поэтому форматируем явно с ограничением до миллисекунд
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
    }

    /**
     * Безопасно парсит объект в BigDecimal
     *
     * @param o Объект для парсинга
     * @return BigDecimal или null, если парсинг не удался
     */
    protected static BigDecimal parseBigDec(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o);
        if (s.isBlank()) return null;
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Безопасно сериализует объект в JSON-строку
     *
     * @param o Объект для сериализации
     * @return JSON-строка или toString() в случае ошибки
     */
    protected String safeJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    /**
     * Проверяет успешность ответа OKX API (код должен быть "0")
     *
     * @param response Ответ от API
     * @return true, если код ответа "0" (успех)
     */
    protected boolean isSuccessResponse(Map<String, Object> response) {
        String code = String.valueOf(response.getOrDefault("code", ""));
        return "0".equals(code);
    }

    /**
     * Извлекает сообщение об ошибке из ответа OKX API
     *
     * @param response Ответ от API
     * @return Сообщение об ошибке
     */
    protected String getErrorMessage(Map<String, Object> response) {
        String code = String.valueOf(response.getOrDefault("code", ""));
        String msg = String.valueOf(response.getOrDefault("msg", "Unknown error"));
        return "Code: " + code + ", message: " + msg;
    }

    /**
     * Освобождает ресурсы HTTP-клиента
     */
    @PreDestroy
    public void shutdown() {
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
    }
}

