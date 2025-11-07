package artskif.trader.indicator;

import artskif.trader.candle.CandleTimeframe;

import java.math.BigDecimal;
import java.time.Instant;

public interface IndicatorPoint {
    BigDecimal getCurrentValue();
    BigDecimal getConfirmedValue();
    IndicatorType getType();
    String getName();
    CandleTimeframe getCandleTimeframe();
    Integer getPeriod();
    Instant getBucket();
    Instant getProcessingTime();
}
