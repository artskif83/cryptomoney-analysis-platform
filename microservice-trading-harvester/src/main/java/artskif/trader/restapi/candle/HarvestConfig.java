package artskif.trader.restapi.candle;

import lombok.Builder;

/**
 * Конфигурация для сбора исторических данных
 */
@Builder
public record HarvestConfig(String instId, int limit, long startEpochMs, long requestPauseMs, int pagesLimit) {
}

