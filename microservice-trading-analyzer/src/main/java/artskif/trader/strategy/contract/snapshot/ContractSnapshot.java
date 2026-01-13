package artskif.trader.strategy.contract.snapshot;

import artskif.trader.candle.CandleTimeframe;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public interface ContractSnapshot {

    /** Идентификатор схемы (Contract.contractHash) */
    String contractHash();

    /** Значение фичи по имени из ContractMetadata */
    Object getFeature(String name);

    /** Все фичи в виде мапы */
    Map<String, Object> getAllFeatures();

    Duration getTimeframe();

    Instant getTimestamp();
}
