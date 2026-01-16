package artskif.trader.executor.okx;

import artskif.trader.executor.orders.positions.ExchangeClient;
import artskif.trader.executor.orders.positions.OrderExecutionResult;
import artskif.trader.executor.orders.model.Symbol;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OkxOrderService implements ExchangeClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String apiSecret;
    private final String passphrase;
    private final String restApiUrl;
    private final OkHttpClient http;

    // основной прод-конструктор (через Spring)
    @Autowired
    public OkxOrderService(
            @Value("${OKX_REST_API:https://www.okx.com}") String restApiUrl,
            @Value("${OKX_API_KEY}") String apiKey,
            @Value("${OKX_API_SECRET}") String apiSecret,
            @Value("${OKX_API_PASSPHRASE}") String passphrase
    ) {
        this.restApiUrl = restApiUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.passphrase = passphrase;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    // доп. конструктор для тестов (без Spring)
    public OkxOrderService(String restApiUrl,
                           String apiKey,
                           String apiSecret,
                           String passphrase,
                           OkHttpClient httpClient) {
        this.restApiUrl = restApiUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.passphrase = passphrase;
        this.http = httpClient;
    }

    // ==== ExchangeClient ====

    @Override
    public OrderExecutionResult placeMarketBuy(Symbol symbol, BigDecimal baseQty) {
        var result = placeSpotMarket(symbol, "buy", baseQty);
        System.out.println("результат, " + result);
        return result;
    }

    @Override
    public OrderExecutionResult placeMarketSell(Symbol symbol, BigDecimal baseQty) {
        var result = placeSpotMarket(symbol, "sell", baseQty);
        System.out.println("результат, " + result);
        return result;
    }

    // ==== Основная логика размещения ордеров через REST API ====

    private OrderExecutionResult placeSpotMarket(Symbol symbol, String side, BigDecimal baseQty) {
        final String clientId = UUID.randomUUID().toString().replace("-", "");
        final String instId = symbol.base() + "-" + symbol.quote();

        // Формируем тело запроса для размещения ордера
        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("instId", instId);
        orderBody.put("tdMode", "cash");
        orderBody.put("side", side);  // buy | sell
        orderBody.put("ordType", "market");
        orderBody.put("sz", baseQty.stripTrailingZeros().toPlainString());
        orderBody.put("tgtCcy", "base_ccy");
        orderBody.put("clOrdId", clientId);

        try {
            String requestBody = mapper.writeValueAsString(orderBody);
            System.out.println("Размещаем ордер через REST API: " + requestBody);

            // Размещаем ордер
            Map<String, Object> response = executeRestRequest("POST", "/api/v5/trade/order", requestBody);

            // Проверяем код ответа
            String code = String.valueOf(response.getOrDefault("code", ""));
            if (!"0".equals(code)) {
                String msg = String.valueOf(response.getOrDefault("msg", "Unknown error"));
                throw new RuntimeException("Order placement failed. Code: " + code + ", message: " + msg);
            }

            // Извлекаем ordId из ответа
            String ordId = null;
            if (response.containsKey("data") && response.get("data") instanceof List<?> list && !list.isEmpty()) {
                Object first = list.getFirst();
                if (first instanceof Map<?, ?> m) {
                    Object ord = m.get("ordId");
                    if (ord != null) ordId = String.valueOf(ord);
                }
            }

            if (ordId == null) {
                throw new RuntimeException("Order placed but ordId not received: " + safeJson(response));
            }

            System.out.println("Ордер размещен, ordId: " + ordId);

            // Получаем детали исполнения ордера с retry-логикой
            BigDecimal avgPrice = null;
            BigDecimal execBase = null;

            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                if (attempt > 0) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                Map<String, Object> orderDetails = getOrderDetails(ordId, instId);

                if (orderDetails != null) {
                    String state = String.valueOf(orderDetails.getOrDefault("state", ""));

                    // Проверяем статус ордера
                    if ("filled".equals(state) || "partially_filled".equals(state)) {
                        avgPrice = parseBigDec(orderDetails.get("avgPx"));
                        execBase = parseBigDec(orderDetails.get("accFillSz"));

                        if (avgPrice != null && execBase != null) {
                            System.out.println("Ордер исполнен: avgPrice=" + avgPrice + ", execBase=" + execBase);
                            break;
                        }
                    } else if ("canceled".equals(state) || "rejected".equals(state)) {
                        throw new RuntimeException("Order was " + state + ": " + safeJson(orderDetails));
                    }
                }
            }

            return new OrderExecutionResult(ordId, avgPrice, execBase);

        } catch (Exception e) {
            throw new RuntimeException("Failed to place spot market order", e);
        }
    }

    // Получение деталей ордера
    private Map<String, Object> getOrderDetails(String ordId, String instId) {
        try {
            String endpoint = "/api/v5/trade/order?ordId=" + ordId + "&instId=" + instId;
            Map<String, Object> response = executeRestRequest("GET", endpoint, null);

            String code = String.valueOf(response.getOrDefault("code", ""));
            if (!"0".equals(code)) {
                System.err.println("Failed to get order details. Code: " + code);
                return null;
            }

            if (response.containsKey("data") && response.get("data") instanceof List<?> list && !list.isEmpty()) {
                Object first = list.getFirst();
                if (first instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) m;
                    return result;
                }
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error getting order details: " + e.getMessage());
            return null;
        }
    }

    // ==== REST API запросы с аутентификацией ====

    private Map<String, Object> executeRestRequest(String method, String endpoint, String body) throws IOException {
        String timestamp = getIsoTimestamp();
        String bodyForSign = (body != null && !body.isEmpty()) ? body : "";
        String sign = generateSignature(timestamp, method, endpoint, bodyForSign);

        // Отладочное логирование
        System.out.println("=== OKX REST API Request ===");
        System.out.println("Timestamp: " + timestamp);
        System.out.println("Timestamp length: " + timestamp.length());
        System.out.println("Method: " + method);
        System.out.println("Endpoint: " + endpoint);
        System.out.println("Body: " + (bodyForSign.isEmpty() ? "<empty>" : bodyForSign));
        System.out.println("PreHash: " + timestamp + method.toUpperCase() + endpoint + bodyForSign);
        System.out.println("Signature: " + sign);
        System.out.println("API Key: " + (apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) + "..." : "null"));
        System.out.println("Passphrase: " + (passphrase != null ? passphrase.substring(0, Math.min(4, passphrase.length())) + "..." : "null"));
        System.out.println("===========================");

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
                throw new IOException("HTTP request failed: " + response.code() + " " + response.message() + ", body: " + responseBody);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(responseBody, Map.class);
            return result;
        }
    }

    // Генерация подписи для REST API (согласно документации OKX)
    private String generateSignature(String timestamp, String method, String endpoint, String body) {
        try {
            // OKX требует: timestamp + method.toUpperCase() + endpoint + body
            String prehash = timestamp + method.toUpperCase() + endpoint + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(prehash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    // Получение ISO timestamp в формате OKX (ISO 8601)
    private String getIsoTimestamp() {
        // OKX требует формат ISO 8601: 2024-01-16T10:30:45.123Z (только миллисекунды!)
        // Instant.toString() может давать микросекунды/наносекунды, что OKX не понимает
        // Поэтому форматируем явно с ограничением до миллисекунд
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
    }

    private static BigDecimal parseBigDec(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o);
        if (s.isBlank()) return null;
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    @PreDestroy
    public void shutdown() {
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
    }
}
