package artskif.trader.executor.okx;

import artskif.trader.executor.orders.positions.ExchangeClient;
import artskif.trader.executor.orders.positions.OrderExecutionResult;
import artskif.trader.executor.orders.strategy.list.Symbol;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import okhttp3.*;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class OkxOrderService implements ExchangeClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2000);

    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String apiSecret;
    private final String passphrase;

    // Состояние одного переиспользуемого сокета
    private volatile WebSocket ws;
    private final Object connectLock = new Object();
    private volatile boolean loggedIn = false;

    // Корреляция запросов "op: order" -> ответ
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingOrders = new ConcurrentHashMap<>();

    // Последние fill-данные по ordId (из подписки "orders")
    private final Map<String, FillSnapshot> fillsByOrdId = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<FillSnapshot>> fillAwaiters = new ConcurrentHashMap<>();
    // настройка таймаута на обогащение (можно вынести в @Value, по умолчанию 5000 мс)
    private static final Duration FILL_ENRICH_TIMEOUT = Duration.ofMillis(5000);

    private record FillSnapshot(BigDecimal avgPrice, BigDecimal execBaseQty) {
    }

    // стало:
    private final String wsUrl;
    private final OkHttpClient http;

    // основной прод-конструктор (через Spring)
    @Autowired
    public OkxOrderService(
            @Value("${OKX_WS_PRIVATE:wss://ws.okx.com:8443/ws/v5/private}") String wsUrl,
            @Value("${OKX_API_KEY}") String apiKey,
            @Value("${OKX_API_SECRET}") String apiSecret,
            @Value("${OKX_API_PASSPHRASE}") String passphrase
    ) {
        this.wsUrl = wsUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.passphrase = passphrase;
        this.http = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build();
    }

    // доп. конструктор для тестов (без Spring)
    public OkxOrderService(String wsUrl,
                           String apiKey,
                           String apiSecret,
                           String passphrase,
                           OkHttpClient httpClient) {
        this.wsUrl = wsUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.passphrase = passphrase;
        this.http = httpClient;
    }

    // ==== ExchangeClient ====

    @Override
    public OrderExecutionResult placeMarketBuy(Symbol symbol, BigDecimal baseQty) {
        var result = placeSpotMarket(symbol, "buy", baseQty);
        System.out.println("результат, %s - fillsByOrdId".formatted(result));
        return result;
    }

    @Override
    public OrderExecutionResult placeMarketSell(Symbol symbol, BigDecimal baseQty) {
        var result = placeSpotMarket(symbol, "sell", baseQty);
        System.out.println("результат, %s - fillsByOrdId".formatted(result));
        return result;
    }

    // ==== Основная синхронная логика ====

    private OrderExecutionResult placeSpotMarket(Symbol symbol, String side, BigDecimal baseQty) {
        ensureConnected();

        final String clientId = UUID.randomUUID().toString().replace("-", "");
        final String instId = symbol.base() + "-" + symbol.quote();

        // формируем args для op: order
        Map<String, Object> args = Map.of(
                "instId", instId,
                "tdMode", "cash",
                "tgtCcy", "base_ccy",
                "side", side,        // buy | sell
                "ordType", "market",
                "sz", baseQty.stripTrailingZeros().toPlainString()
        );
        Map<String, Object> orderReq = Map.of(
                "id", clientId,
                "op", "order",
                "args", new Object[]{args}
        );

        CompletableFuture<Map<String, Object>> waiter = new CompletableFuture<>();
        pendingOrders.put(clientId, waiter);

        try {
            System.out.println("отправляем ордер - %s".formatted(mapper.writeValueAsString(orderReq)));

            ws.send(mapper.writeValueAsString(orderReq));
        } catch (Exception e) {
            pendingOrders.remove(clientId);
            throw new RuntimeException("WS send failed", e);
        }

        Map<String, Object> reply;
        try {
            // ждём ответ именно на наш op: order
            reply = waiter.get(DEFAULT_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            pendingOrders.remove(clientId);
            throw new RuntimeException("Timeout or interruption waiting order reply", e);
        }

        // Разбор ответа
        System.out.println("Разбор ответа на order");

        String code = String.valueOf(reply.getOrDefault("code", ""));
        String ordId = null;
        if (reply.containsKey("data") && reply.get("data") instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> m) {
                Object sCode = m.get("sCode");
                if (sCode != null) code = String.valueOf(sCode);
                Object ord = m.get("ordId");
                if (ord != null) ordId = String.valueOf(ord);
            }
        }

        if (!"0".equals(code)) {
            throw new RuntimeException("Order error code: " + code + " raw=" + safeJson(reply));
        }

        System.out.println("Пытаемся обогатить результат мгновенными fill-данными (если они уже прилетели)");

        // Пытаемся обогатить результат мгновенными fill-данными (если они уже прилетели)
        BigDecimal avgPrice = null;
        BigDecimal execBase = null;
        if (ordId != null) {
            // есть готовый снапшот?
            FillSnapshot snap = fillsByOrdId.get(ordId);
            if (snap == null) {
                // ждём чуть-чуть пуш статуса по этому ордеру
                CompletableFuture<FillSnapshot> f =
                        fillAwaiters.computeIfAbsent(ordId, k -> new CompletableFuture<>());
                try {
                    snap = f.get(FILL_ENRICH_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                } catch (Exception ignore) {
                    // не успело — вернёмся без обогащения
                } finally {
                    fillAwaiters.remove(ordId);
                }
            }
            if (snap != null) {
                avgPrice = snap.avgPrice();
                execBase = snap.execBaseQty();
            }
        }

        System.out.println("Отдаем обогащенный ответ - %s code, %s ordId, %s - fillsByOrdId".formatted(code, ordId, new OrderExecutionResult(ordId, avgPrice, execBase)));

        return new OrderExecutionResult(ordId, avgPrice, execBase);
    }

    // ==== Подключение/логин и обработчик сообщений ====

    private void ensureConnected() {
        if (ws != null && loggedIn) return;
        synchronized (connectLock) {
            if (ws != null && loggedIn) return;

            CompletableFuture<Boolean> loginAck = new CompletableFuture<>();

            Request req = new Request.Builder()
                    .url(wsUrl)
                    .addHeader("OK-ACCESS-KEY", apiKey)
                    .addHeader("OK-ACCESS-SIGN", signForVerify(ts()))
                    .addHeader("OK-ACCESS-TIMESTAMP", ts())
                    .addHeader("OK-ACCESS-PASSPHRASE", passphrase)
                    .build();

            ws = http.newWebSocket(req, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    try {
                        // Отправляем login
                        System.out.println("Коннектимся к OKX %s");

                        String t = ts();
                        Map<String, Object> login = Map.of(
                                "op", "login",
                                "args", new Object[]{Map.of(
                                        "apiKey", apiKey,
                                        "passphrase", passphrase,
                                        "timestamp", t,
                                        "sign", signForVerify(t)
                                )}
                        );
                        webSocket.send(mapper.writeValueAsString(login));
                    } catch (Exception e) {
                        loginAck.completeExceptionally(e);
                        webSocket.close(1001, "login build failed");
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    try {
                        Map<String, Object> msg = mapper.readValue(text, Map.class);

                        // login ok?
                        if (Objects.equals(msg.get("event"), "login")) {
                            System.out.println("ответ на login");

                            boolean ok = Objects.equals(String.valueOf(msg.get("code")), "0");
                            loggedIn = ok;
                            if (ok) {
                                // подписка на заказы — хотим ловить fills
                                Map<String, Object> sub = Map.of(
                                        "op", "subscribe",
                                        "args", new Object[]{Map.of(
                                                "channel", "orders",
                                                "instType", "SPOT"
                                        )}
                                );
                                webSocket.send(mapper.writeValueAsString(sub));
                                loginAck.complete(true);
                                System.out.println("подписались на orders");
                            } else {
                                loginAck.completeExceptionally(new RuntimeException("Login failed: " + text));
                            }
                            return;
                        }

                        // ответы на размещение ордеров
                        if (Objects.equals(msg.get("op"), "order")) {
                            String clientId = String.valueOf(msg.getOrDefault("id", ""));
                            CompletableFuture<Map<String, Object>> fut = pendingOrders.remove(clientId);
                            if (fut != null) fut.complete(msg);
                            System.out.println("ответ на размещение ордер - %s".formatted(clientId));
                            return;
                        }

                        // стрим статусов ордеров (channel: orders)
                        if (Objects.equals(msg.get("arg") instanceof Map ? ((Map<?, ?>) msg.get("arg")).get("channel") : null, "orders")) {
                            System.out.println("пришел ответ с orders %s".formatted(msg.get("data")));

                            Object data = msg.get("data");
                            if (data instanceof List<?> list) {
                                for (Object o : list) {
                                    if (o instanceof Map<?, ?> m) {
                                        String ordId = String.valueOf(m.get("ordId"));

                                        BigDecimal avgPx = parseBigDec(m.get("avgPx"));
                                        BigDecimal accFillSz = parseBigDec(m.get("accFillSz"));
                                        BigDecimal fillPx = parseBigDec(m.get("fillPx"));
                                        BigDecimal fillSz = parseBigDec(m.get("fillSz"));
                                        String state = String.valueOf(m.get("state"));

                                        FillSnapshot snap = null;
                                        if (avgPx != null && accFillSz != null) {
                                            snap = new FillSnapshot(avgPx, accFillSz);
                                        } else if (fillPx != null && fillSz != null) {
                                            snap = new FillSnapshot(fillPx, fillSz);
                                        }

                                        if (ordId != null && snap != null && Objects.equals(state, "filled")) {
                                            fillsByOrdId.put(ordId, snap);
                                            CompletableFuture<FillSnapshot> f = fillAwaiters.remove(ordId);
                                            if (f != null) f.complete(snap);
                                        }
                                    }
                                }
                            }
                            return;
                        }
                    } catch (Exception ignore) {
                        // игнорируем пинги/прочие служебные сообщения
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) { /* ignore */ }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    loggedIn = false;
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    loggedIn = false;
                    // фейлим все висящие ордера
                    pendingOrders.forEach((k, f) -> f.completeExceptionally(t));
                    pendingOrders.clear();
                }
            });

            try {
                // ждём подтверждение логина
                loginAck.get(DEFAULT_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new RuntimeException("OKX WS login failed/timeout", e);
            }
        }
    }

    private String ts() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    private String signForVerify(String ts) {
        try {
            String prehash = ts + "GET" + "/users/self/verify";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(prehash.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        try {
            if (ws != null) ws.close(1000, "shutdown");
        } catch (Exception ignore) {
        }
        http.dispatcher().executorService().shutdownNow();
        http.connectionPool().evictAll();
    }
}
