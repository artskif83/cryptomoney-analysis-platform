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

    /**
     * Возвращает полный баланс счёта в USDT с учётом всех монет и открытых позиций (unrealized PnL).
     * Использует поле totalEq из /api/v5/account/balance.
     * @return Суммарный эквивалент счёта в USDT
     */
    BigDecimal getTotalEquityInUsdt() throws Exception;

}
