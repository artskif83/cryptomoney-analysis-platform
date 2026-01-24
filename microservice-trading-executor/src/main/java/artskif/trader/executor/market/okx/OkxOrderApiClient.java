package artskif.trader.executor.market.okx;

import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.executor.orders.AccountClient;
import artskif.trader.executor.orders.OrdersClient;
import artskif.trader.executor.common.Symbol;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class OkxOrderApiClient extends OkxApiClient implements OrdersClient {

    private static final Logger log = LoggerFactory.getLogger(OkxOrderApiClient.class);

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final AccountClient accountClient;

    // основной прод-конструктор (через Spring)
    @Autowired
    public OkxOrderApiClient(OkxConfig config, AccountClient accountClient) {
        super(config.getRestApiUrl(), config.getApiKey(), config.getApiSecret(), config.getPassphrase());
        this.accountClient = accountClient;
    }

    // доп. конструктор для тестов (без Spring)
    public OkxOrderApiClient(String restApiUrl,
                             String apiKey,
                             String apiSecret,
                             String passphrase,
                             OkHttpClient httpClient,
                             AccountClient accountClient) {
        super(restApiUrl, apiKey, apiSecret, passphrase, httpClient);
        this.accountClient = accountClient;
    }

    // ==== ExchangeClient ====

    /**
     * Покупка по рынку на спотовом рынке.
     * @param symbol Торговая пара
     * @param percentOfDeposit Процент от депозита в квотируемой валюте (от 0 до 100)
     * @return Результат исполнения ордера
     */
    @Override
    public OrderExecutionResult placeSpotMarketBuy(Symbol symbol, BigDecimal percentOfDeposit) {
        // Получаем баланс квотируемой валюты (например, USDT)
        BigDecimal quoteBalance = accountClient.getCurrencyBalance(symbol.quote());
        if (quoteBalance == null || quoteBalance.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("❌ Недостаточный баланс {} для покупки", symbol.quote());
            return null;
        }

        // Вычисляем размер ордера как процент от баланса
        BigDecimal orderSize = quoteBalance
                .multiply(percentOfDeposit)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.DOWN);

        log.info("💰 Баланс {}: {}, процент: {}%, размер ордера: {}",
                symbol.quote(), quoteBalance, percentOfDeposit, orderSize);

        var result = placeSpotMarket(symbol, "buy", orderSize, true);
        log.info("📊 Результат покупки: {}", result);
        return result;
    }

    /**
     * Продажа по рынку на спотовом рынке.
     * @param symbol Торговая пара
     * @param percentOfDeposit Процент от депозита в базовой валюте (от 0 до 100)
     * @return Результат исполнения ордера
     */
    @Override
    public OrderExecutionResult placeSpotMarketSell(Symbol symbol, BigDecimal percentOfDeposit) {
        // Получаем баланс базовой валюты (например, BTC)
        BigDecimal baseBalance = accountClient.getCurrencyBalance(symbol.base());
        if (baseBalance == null || baseBalance.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("❌ Недостаточный баланс {} для продажи", symbol.base());
            return null;
        }

        // Вычисляем размер ордера как процент от баланса
        BigDecimal orderSize = baseBalance
                .multiply(percentOfDeposit)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.DOWN);

        log.info("💰 Баланс {}: {}, процент: {}%, размер ордера: {}",
                symbol.base(), baseBalance, percentOfDeposit, orderSize);

        var result = placeSpotMarket(symbol, "sell", orderSize, false);
        log.info("📊 Результат продажи: {}", result);
        return result;
    }

    // ==== Основная логика размещения ордеров через REST API ====

    /**
     * Размещает рыночный ордер на спотовом рынке.
     * @param symbol Торговая пара
     * @param side "buy" или "sell"
     * @param size Размер ордера
     * @param isQuoteCurrency true - размер указан в квотируемой валюте, false - в базовой валюте
     * @return Результат исполнения ордера
     */
    private OrderExecutionResult placeSpotMarket(Symbol symbol, String side, BigDecimal size, boolean isQuoteCurrency) {
        final String clientId = UUID.randomUUID().toString().replace("-", "");
        final String instId = symbol.base() + "-" + symbol.quote();

        // Формируем тело запроса для размещения ордера
        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("instId", instId);
        orderBody.put("tdMode", "cash");
        orderBody.put("side", side);  // buy | sell
        orderBody.put("ordType", "market");
        orderBody.put("sz", size.stripTrailingZeros().toPlainString());

        // Для покупки указываем размер в квотируемой валюте (например, USDT)
        // Для продажи указываем размер в базовой валюте (например, BTC)
        if (isQuoteCurrency) {
            orderBody.put("tgtCcy", "quote_ccy");  // размер ордера в quote-валюте
        } else {
            orderBody.put("tgtCcy", "base_ccy");  // размер ордера в base-валюте
        }

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
                log.error("❌ Ордер размещен, но ordId не получен: {}", ordId);
                throw new RuntimeException("Ордер размещен, но ordId не получен: " + safeJson(response));
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
                        log.error("❌ Ордер был: {}", state + ": " + safeJson(orderDetails));
                        throw new RuntimeException("Ордер был " + state + ": " + safeJson(orderDetails));
                    }
                }
            }

            return new OrderExecutionResult(ordId, avgPrice, execBase);

        } catch (Exception e) {
            log.error("❌ Не удалось разместить ордер на спотовом рынке");
        }
        return null;
    }

    // Получение деталей ордера
    private Map<String, Object> getOrderDetails(String ordId, String instId) {
        try {
            String endpoint = "/api/v5/trade/order?ordId=" + ordId + "&instId=" + instId;
            Map<String, Object> response = executeRestRequest("GET", endpoint, null);

            if (!isSuccessResponse(response)) {
                log.error("❌ Не удалось получить информацию о заказе. {}", getErrorMessage(response));
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
            log.error("❌ Ошибка получения информации о заказе: {}", e.getMessage(), e);
            return null;
        }
    }
}
