package artskif.trader.executor.market.okx;

import artskif.trader.executor.orders.OrdersClient;
import artskif.trader.executor.orders.OrderExecutionResult;
import artskif.trader.executor.common.Symbol;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OkxOrderService extends OkxApiClient implements OrdersClient {

    private static final Logger log = LoggerFactory.getLogger(OkxOrderService.class);

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    // основной прод-конструктор (через Spring)
    @Autowired
    public OkxOrderService(
            @Value("${OKX_REST_API:https://www.okx.com}") String restApiUrl,
            @Value("${OKX_API_KEY}") String apiKey,
            @Value("${OKX_API_SECRET}") String apiSecret,
            @Value("${OKX_API_PASSPHRASE}") String passphrase
    ) {
        super(restApiUrl, apiKey, apiSecret, passphrase);
    }

    // доп. конструктор для тестов (без Spring)
    public OkxOrderService(String restApiUrl,
                           String apiKey,
                           String apiSecret,
                           String passphrase,
                           OkHttpClient httpClient) {
        super(restApiUrl, apiKey, apiSecret, passphrase, httpClient);
    }

    // ==== ExchangeClient ====

    @Override
    public OrderExecutionResult placeMarketBuy(Symbol symbol, BigDecimal baseQty) {
        var result = placeSpotMarket(symbol, "buy", baseQty);
        log.info("📊 Результат покупки: {}", result);
        return result;
    }

    @Override
    public OrderExecutionResult placeMarketSell(Symbol symbol, BigDecimal baseQty) {
        var result = placeSpotMarket(symbol, "sell", baseQty);
        log.info("📊 Результат продажи: {}", result);
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

            // Размещаем ордер
            Map<String, Object> response = executeRestRequest("POST", "/api/v5/trade/order", requestBody);

            // Проверяем код ответа
            if (!isSuccessResponse(response)) {
                throw new RuntimeException("Order placement failed. " + getErrorMessage(response));
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

            log.info("✅ Ордер размещен, ordId: {}", ordId);

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
                            log.info("✅ Ордер исполнен: avgPrice={}, execBase={}", avgPrice, execBase);
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

            if (!isSuccessResponse(response)) {
                log.error("❌ Failed to get order details. {}", getErrorMessage(response));
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
            log.error("❌ Error getting order details: {}", e.getMessage(), e);
            return null;
        }
    }
}
