package okx;


import artskif.trader.executor.okx.OkxOrderService;
import artskif.trader.executor.orders.positions.OrderExecutionResult;
import artskif.trader.executor.orders.strategy.list.Symbol;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OkxOrderServiceTest {

    private MockWebServer server;
    private OkxOrderService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build();

        String wsUrl = server.url("/ws/v5/private").toString().replace("http", "ws");
        service = new OkxOrderService(
                wsUrl,
                "testKey",
                "testSecret",
                "testPass",
                client
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void placeMarketBuy_success_returnsOrdId_and_enrichesWithFill() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) { /* noop */ }

            @Override public void onMessage(WebSocket webSocket, String text) {
                try {
                    Map<?,?> msg = mapper.readValue(text, Map.class);

                    // 1) login
                    if ("login".equals(msg.get("op"))) {
                        webSocket.send("{\"event\":\"login\",\"code\":\"0\"}");
                        return;
                    }

                    // 2) order request -> reply + push 'orders'
                    if ("order".equals(msg.get("op"))) {
                        String id = String.valueOf(msg.get("id"));
                        String orderReply = """
                            {"id":"%s","op":"order","code":"0",
                             "data":[{"sCode":"0","ordId":"1234567890"}]}
                        """.formatted(id);
                        webSocket.send(orderReply);

                        // ВАЖНО: теперь нужен state:"filled" иначе сервис не положит снапшот
                        String ordersPush = """
                            {"arg":{"channel":"orders","instType":"SPOT"},
                             "data":[{"ordId":"1234567890","avgPx":"27000.1","accFillSz":"0.005","state":"filled"}]}
                        """;
                        webSocket.send(ordersPush);
                    }
                } catch (Exception ignore) {}
            }

            @Override public void onMessage(WebSocket webSocket, ByteString bytes) { /* ignore */ }
        }));

        OrderExecutionResult result =
                service.placeMarketBuy(new Symbol("BTC","USDT"), new BigDecimal("0.005"));

        assertThat(result.exchangeOrderId()).isEqualTo("1234567890");
        assertThat(result.avgPrice()).isEqualByComparingTo("27000.1");
        assertThat(result.executedBaseQty()).isEqualByComparingTo("0.005");
    }

    @Test
    void placeMarketSell_error_minNotional_throws() {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
            @Override public void onMessage(WebSocket webSocket, String text) {
                try {
                    Map<?,?> msg = mapper.readValue(text, Map.class);

                    if ("login".equals(msg.get("op"))) {
                        webSocket.send("{\"event\":\"login\",\"code\":\"0\"}");
                        return;
                    }
                    if ("order".equals(msg.get("op"))) {
                        String id = String.valueOf(msg.get("id"));
                        String reply = """
                            {"id":"%s","op":"order","code":"0",
                             "data":[{"sCode":"51020","sMsg":"Your order should meet or exceed the minimum order amount."}]}
                        """.formatted(id);
                        webSocket.send(reply);
                    }
                } catch (Exception ignore) {}
            }
        }));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.placeMarketSell(new Symbol("BTC","USDT"), new BigDecimal("0.00001"))
        );
        assertThat(ex).hasMessageContaining("51020");
    }

    @Test
    void login_failed_throwsImmediately() {
        // Вместо огромного таймаута на ожидание order-реплая — проваливаем логин
        server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) { /* noop */ }
            @Override public void onMessage(WebSocket webSocket, String text) {
                try {
                    Map<?,?> msg = mapper.readValue(text, Map.class);
                    if ("login".equals(msg.get("op"))) {
                        webSocket.send("{\"event\":\"login\",\"code\":\"1\",\"msg\":\"bad creds\"}");
                    }
                } catch (Exception ignore) {}
            }
        }));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.placeMarketBuy(new Symbol("BTC","USDT"), new BigDecimal("0.001"))
        );
        assertThat(ex.getMessage()).contains("OKX WS login failed/timeout");
    }
}
