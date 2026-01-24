package artskif.trader.executor.orders;

import java.math.BigDecimal;

public interface AccountClient {
    /**
     * Получает доступный баланс USDT на торговом аккаунте
     * @return Баланс USDT или null в случае ошибки
     */
    BigDecimal getUsdtBalance();

    /**
     * Получает доступный баланс указанной валюты на торговом аккаунте
     * @param currency Код валюты (например, "USDT", "BTC", "ETH")
     * @return Баланс указанной валюты или null в случае ошибки
     */
    BigDecimal getCurrencyBalance(String currency);
}
