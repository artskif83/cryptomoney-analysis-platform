package artskif.trader.indicator;

import artskif.trader.candle.CandleTimeframe;

import java.time.Instant;

public interface IndicatorPoint<C> {
    IndicatorType getType();
    String getName();
    CandleTimeframe getCandleTimeframe();
    Integer getPeriod();
    Instant getBucket();
    Instant getProcessingTime();
}
