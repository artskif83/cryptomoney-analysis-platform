package artskif.trader.executor.orders;

import java.math.BigDecimal;

public interface AccountClient {
    /**
     * Получает доступный баланс USDT на торговом аккаунте
     * @return Баланс USDT или null в случае ошибки
     */
    BigDecimal getUsdtBalance();
}
