package artskif.trader.microserviceokxexecutor.okx;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import okhttp3.*;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class OkxOrderService {

    private final OkHttpClient http = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);

    @Value("${OKX_API_KEY}")        private String apiKey;
    @Value("${OKX_API_SECRET}")     private String apiSecret;
    @Value("${OKX_API_PASSPHRASE}") private String passphrase;

    private static final String WS_PRIVATE_LIVE = "wss://ws.okx.com:8443/ws/v5/private";

    private String ts() { return String.valueOf(System.currentTimeMillis() / 1000); }

    private String signForVerify(String ts) {
        try {
            String prehash = ts + "GET" + "/users/self/verify";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.Base64.getEncoder().encodeToString(mac.doFinal(prehash.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Создаёт один SPOT Market-ордер в отдельном потоке (боевое окружение).
     * @param instId пример "BTC-USDT"
     * @param side   "buy" или "sell"
     * @param sz     размер (кол-во базовой монеты)
     * @return CompletableFuture с ordId (если вернулся), иначе пусто
     */
    public CompletableFuture<Optional<String>> placeSpotMarketOrderAsync(String instId, String side, String sz) {
        CompletableFuture<Optional<String>> result = new CompletableFuture<>();

        String ts = ts(), sign = signForVerify(ts);
        Request req = new Request.Builder()
                .url(WS_PRIVATE_LIVE)
                .addHeader("OK-ACCESS-KEY", apiKey)
                .addHeader("OK-ACCESS-SIGN", sign)
                .addHeader("OK-ACCESS-TIMESTAMP", ts)
                .addHeader("OK-ACCESS-PASSPHRASE", passphrase)
                .build();

        WebSocket ws = http.newWebSocket(req, new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) {
                try {
                    String ts2 = ts(), sign2 = signForVerify(ts2);
                    Map<String,Object> login = Map.of(
                            "op","login",
                            "args", new Object[]{ Map.of(
                                    "apiKey", apiKey,
                                    "passphrase", passphrase,
                                    "timestamp", ts2,
                                    "sign", sign2
                            )}
                    );
                    webSocket.send(new ObjectMapper().writeValueAsString(login));
                } catch (Exception e) {
                    result.completeExceptionally(e);
                    webSocket.close(1001, "login build failed");
                }
            }

            @Override public void onMessage(WebSocket webSocket, String text) {
                try {
                    Map<String, Object> msg = mapper.readValue(text, Map.class); // будет warning про unchecked, это ок

                    if (Objects.equals(msg.get("event"), "login") && Objects.equals(msg.get("code"), "0")) {
                        // Подписка на ордера (для статусов)
//                        Map<String,Object> sub = Map.of(
//                                "op","subscribe",
//                                "args", new Object[]{ Map.of("channel","orders","instType","SPOT") }
//                        );
//                        webSocket.send(mapper.writeValueAsString(sub));

                        // Размещение SPOT market ордера
                        Map<String,Object> order = Map.of(
                                "id","1251",
                                "op","order",
                                "args", new Object[]{ Map.of(
                                        "instId", instId,
                                        "tdMode", "cash",
                                        "tgtCcy",  "base_ccy",
                                        "side", side,         // buy | sell
                                        "ordType","market",
                                        "sz", sz
                                )}
                        );
                        webSocket.send(mapper.writeValueAsString(order));
                    }

                    if (Objects.equals(msg.get("op"), "order")) {
                        // Унифицированная обработка ответа на размещение
                        String code = String.valueOf(msg.getOrDefault("code", ""));
                        Optional<String> ordId = Optional.empty();

                        if (msg.containsKey("data") && msg.get("data") instanceof java.util.List<?> list && !list.isEmpty()) {
                            Object first = list.get(0);
                            if (first instanceof Map<?,?> m && m.get("ordId") != null) {
                                ordId = Optional.of(String.valueOf(m.get("ordId")));
                            }
                            Object sCode = (first instanceof Map<?,?> m) ? m.get("sCode") : null;
                            if (sCode != null) code = String.valueOf(sCode);
                        }

                        if ("0".equals(code)) {
                            result.complete(ordId);
                            webSocket.close(1000, "done");
                        } else if (!"".equals(code)) {
                            result.completeExceptionally(new RuntimeException("Order error code: " + code + " raw=" + text));
                            webSocket.close(1002, "order error");
                        }
                    }

                } catch (Exception ignore
                ) { /* другие сообщения: пинги/канальные */ }
            }

            @Override public void onMessage(WebSocket webSocket, ByteString bytes) { /* ignore */ }

            @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                if (!result.isDone()) result.completeExceptionally(t);
            }

            @Override public void onClosed(WebSocket webSocket, int code, String reason) {
                if (!result.isDone()) result.complete(Optional.empty());
            }
        });

        // Страховочный таймер
        exec.schedule(() -> {
            if (!result.isDone()) {
                result.completeExceptionally(new TimeoutException("WS flow timeout"));
                ws.close(1000, "timeout");
            }
        }, 20, TimeUnit.SECONDS);

        return result;
    }

    @PreDestroy
    public void shutdown() {
        exec.shutdownNow();
        http.dispatcher().executorService().shutdownNow();
        http.connectionPool().evictAll();
    }

}
