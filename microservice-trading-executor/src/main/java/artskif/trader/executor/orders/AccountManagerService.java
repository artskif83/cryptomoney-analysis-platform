package artskif.trader.executor.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public final class AccountManagerService {

    private static final Logger log = LoggerFactory.getLogger(AccountManagerService.class);

    private final AccountClient accountClient;
    private final OrdersClient ordersClient;

    public AccountManagerService(AccountClient accountClient, OrdersClient ordersClient) {
        this.accountClient = accountClient;
        this.ordersClient = ordersClient;
    }

    /**
     * Получает доступный баланс USDT на торговом аккаунте
     * @return Баланс USDT
     */
    public BigDecimal getUsdtBalance() throws Exception {
        log.debug("💰 Запрос баланса USDT");
        BigDecimal balance = accountClient.getUsdtBalance();
        return balance;
    }

    /**
     * Получает историю закрытых SWAP позиций по инструменту.
     *
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP"), может быть null
     * @param before Unix timestamp в миллисекундах (строка), может быть null
     * @return Список записей истории позиций
     */
    public List<Map<String, Object>> getPositionsHistory(String instId, String before) throws Exception {
        log.debug("📋 Запрос истории позиций: instId={}, before={}", instId, before);
        return ordersClient.getPositionsHistory(instId, before);
    }
}
