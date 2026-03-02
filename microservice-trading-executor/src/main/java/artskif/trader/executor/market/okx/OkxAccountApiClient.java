package artskif.trader.executor.market.okx;

import artskif.trader.executor.orders.AccountClient;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class OkxAccountApiClient extends OkxApiClient implements AccountClient {

    private static final Logger log = LoggerFactory.getLogger(OkxAccountApiClient.class);

    // Основной прод-конструктор (через Spring)
    @Autowired
    public OkxAccountApiClient(OkxConfig config) {
        super(config.getRestApiUrl(), config.getApiKey(), config.getApiSecret(), config.getPassphrase());
    }

    // Дополнительный конструктор для тестов (без Spring)
    public OkxAccountApiClient(String restApiUrl,
                               String apiKey,
                               String apiSecret,
                               String passphrase,
                               OkHttpClient httpClient) {
        super(restApiUrl, apiKey, apiSecret, passphrase, httpClient);
    }

    @Override
    public BigDecimal getUsdtBalance() {
        return getCurrencyBalance("USDT");
    }

    @Override
    public BigDecimal getCurrencyBalance(String currency) {
        try {
            // Запрос баланса по API OKX: /api/v5/account/balance
            String endpoint = "/api/v5/account/balance";
            Map<String, Object> response = executeRestRequest("GET", endpoint, null);

            // Проверяем код ответа
            if (!isSuccessResponse(response)) {
                log.error("❌ Не удалось получить баланс аккаунта. {}", getErrorMessage(response));
                return null;
            }

            // Извлекаем данные о балансе
            if (response.containsKey("data") && response.get("data") instanceof List<?> dataList && !dataList.isEmpty()) {
                Object first = dataList.getFirst();
                if (first instanceof Map<?, ?> accountData) {
                    // Получаем список балансов по валютам
                    Object detailsObj = accountData.get("details");
                    if (detailsObj instanceof List<?> details) {
                        for (Object detailObj : details) {
                            if (detailObj instanceof Map<?, ?> detail) {
                                String ccy = String.valueOf(detail.get("ccy"));
                                if (currency.equals(ccy)) {
                                    // availBal - доступный баланс для торговли
                                    BigDecimal availBalance = parseBigDec(detail.get("availBal"));
                                    if (availBalance != null) {
                                        log.info("💰 Доступный баланс {}: {}", currency, availBalance);
                                        return availBalance;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            log.warn("⚠️ {} баланс не найден в ответе API", currency);
            return BigDecimal.ZERO;

        } catch (Exception e) {
            log.error("❌ Ошибка при получении баланса {}: {}", currency, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Получает историю закрытых позиций по инструменту через /api/v5/account/positions-history.
     * instType фиксирован = SWAP, возвращаются все закрытые позиции (type не передаётся — API
     * по умолчанию возвращает все типы закрытия).
     *
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP")
     * @param before Unix timestamp в миллисекундах (строка); возвращаются записи,
     *               обновлённые позже указанного момента (фильтр по полю uTime)
     * @return Список записей истории позиций или пустой список в случае ошибки
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPositionsHistory(String instId, String before) {
        try {
            // Формируем endpoint с query-параметрами
            StringBuilder endpoint = new StringBuilder("/api/v5/account/positions-history?instType=SWAP");
            if (instId != null && !instId.isBlank()) {
                endpoint.append("&instId=").append(instId).append("-SWAP");
            }
            if (before != null && !before.isBlank()) {
                endpoint.append("&before=").append(before);
            }

            String fullEndpoint = endpoint.toString();
            log.debug("📋 Запрос истории позиций: {}", fullEndpoint);

            Map<String, Object> response = executeRestRequest("GET", fullEndpoint, null);

            if (!isSuccessResponse(response)) {
                log.error("❌ Не удалось получить историю позиций. {}", getErrorMessage(response));
                return null;
            }

            Object dataObj = response.get("data");
            if (dataObj instanceof List<?> dataList) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object item : dataList) {
                    if (item instanceof Map<?, ?> entry) {
                        result.add((Map<String, Object>) entry);
                    }
                }
                log.info("📋 История позиций {}: получено {} записей", instId, result.size());
                return result;
            }

            log.warn("⚠️ История позиций не найдена в ответе API для инструмента {}", instId);
            return null;

        } catch (Exception e) {
            log.error("❌ Ошибка при получении истории позиций {}: {}", instId, e.getMessage(), e);
            return null;
        }
    }
}

