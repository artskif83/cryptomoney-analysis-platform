package artskif.trader.executor.market.okx;

import artskif.trader.executor.orders.AccountClient;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    public BigDecimal getUsdtBalance() throws Exception {
        return getCurrencyBalance("USDT");
    }

    @Override
    public BigDecimal getCurrencyBalance(String currency) throws Exception {
        String endpoint = "/api/v5/account/balance";
        Map<String, Object> response = executeRestRequest("GET", endpoint, null);

        if (!isSuccessResponse(response)) {
            throw new RuntimeException("Не удалось получить баланс аккаунта. " + getErrorMessage(response));
        }

        if (response.containsKey("data") && response.get("data") instanceof List<?> dataList && !dataList.isEmpty()) {
            Object first = dataList.getFirst();
            if (first instanceof Map<?, ?> accountData) {
                Object detailsObj = accountData.get("details");
                if (detailsObj instanceof List<?> details) {
                    for (Object detailObj : details) {
                        if (detailObj instanceof Map<?, ?> detail) {
                            String ccy = String.valueOf(detail.get("ccy"));
                            if (currency.equals(ccy)) {
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
    }

    @Override
    public BigDecimal getTotalEquityInUsdt() throws Exception {
        String endpoint = "/api/v5/account/balance";
        Map<String, Object> response = executeRestRequest("GET", endpoint, null);

        if (!isSuccessResponse(response)) {
            throw new RuntimeException("Не удалось получить баланс аккаунта. " + getErrorMessage(response));
        }

        if (response.containsKey("data") && response.get("data") instanceof List<?> dataList && !dataList.isEmpty()) {
            Object first = dataList.getFirst();
            if (first instanceof Map<?, ?> accountData) {
                BigDecimal totalEq = parseBigDec(accountData.get("totalEq"));
                if (totalEq != null) {
                    log.info("💹 Суммарный баланс счёта (totalEq): {} USDT", totalEq);
                    return totalEq;
                }
            }
        }

        log.warn("⚠️ totalEq не найден в ответе API, расчёт по деталям");
        return calculateTotalEquityFromDetails(response);
    }

    /**
     * Резервный расчёт: суммирует eqUsd по всем монетам из details, если totalEq недоступен.
     */
    private BigDecimal calculateTotalEquityFromDetails(Map<String, Object> response) {
        BigDecimal total = BigDecimal.ZERO;
        if (response.get("data") instanceof List<?> dataList && !dataList.isEmpty()
                && dataList.getFirst() instanceof Map<?, ?> accountData
                && accountData.get("details") instanceof List<?> details) {
            for (Object detailObj : details) {
                if (detailObj instanceof Map<?, ?> detail) {
                    BigDecimal eqUsd = parseBigDec(detail.get("eqUsd"));
                    if (eqUsd != null) {
                        total = total.add(eqUsd);
                    }
                }
            }
        }
        log.info("💹 Суммарный баланс (расчёт по eqUsd): {} USDT", total);
        return total;
    }
}
