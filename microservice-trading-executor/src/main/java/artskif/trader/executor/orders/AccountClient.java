package artskif.trader.executor.orders;

import java.math.BigDecimal;

public interface AccountClient {
    /**
     * Получает доступный баланс USDT на торговом аккаунте
     * @return Баланс USDT или null в случае ошибки
     */
    BigDecimal getUsdtBalance() throws Exception;

    /**
     * Получает доступный баланс указанной валюты на торговом аккаунте
     * @param currency Код валюты (например, "USDT", "BTC", "ETH")
     * @return Баланс указанной валюты
     */
    BigDecimal getCurrencyBalance(String currency) throws Exception;


}
