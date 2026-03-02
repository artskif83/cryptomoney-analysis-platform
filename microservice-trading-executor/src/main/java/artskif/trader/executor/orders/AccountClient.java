package artskif.trader.executor.orders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    /**
     * Получает историю закрытых позиций по инструменту.
     * По умолчанию instType = SWAP, type = все закрытые позиции.
     *
     * @param instId Идентификатор инструмента (например, "BTC-USDT-SWAP")
     * @param before Фильтр по времени: возвращать записи с временем обновления строго позже
     *               этого значения (Unix timestamp в миллисекундах в виде строки)
     * @return Список записей истории позиций или пустой список в случае ошибки
     */
    List<Map<String, Object>> getPositionsHistory(String instId, String before);
}
