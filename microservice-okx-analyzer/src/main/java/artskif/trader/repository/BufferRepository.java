package artskif.trader.repository;

import artskif.trader.candle.CandleTimeframe;

import java.time.Instant;
import java.util.Map;

/**
 * Дополнительные операции репозитория свечей.
 */
public interface BufferRepository<C> {

    boolean saveFromMap(Map<Instant, C> buffer);

    Map<Instant, C> restoreFromStorage(CandleTimeframe timeframe, String symbol);

}
