package artskif.trader.strategy.snapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public interface DatabaseSnapshot {

    /** Идентификатор схемы (Contract.contractHash) */
    String contractHash();

       /** Все фичи в виде мапы */
    Map<String, Object> getAllColumns();

    Duration getTimeframe();

    Instant getTimestamp();
}
