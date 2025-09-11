package artskif.trader.indicator;

import artskif.trader.candle.CandleTimeframe;

import java.math.BigDecimal;
import java.time.Instant;

public interface IndicatorPoint {
    BigDecimal getValue();
    BigDecimal getLastValue();
    IndicatorType getType();
    String getName();
    CandleTimeframe getCandleTimeframe();
    Integer getPeriod();
    Instant getBucket();
    Instant getTs();
}
