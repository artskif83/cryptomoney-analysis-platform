package artskif.trader.restapi.core;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/**
 * Базовый интерфейс для REST API клиентов криптовалютных бирж
 */
public interface CryptoRestApiClient<C> {
    /**
     * Получить свечные данные
     * @param request запрос на получение свечей
     * @return JSON ответ или empty если произошла ошибка
     */
    Optional<JsonNode> fetchCandles(C request);

    /**
     * Получить имя провайдера (OKX, Binance, etc.)
     */
    String getProviderName();
}

