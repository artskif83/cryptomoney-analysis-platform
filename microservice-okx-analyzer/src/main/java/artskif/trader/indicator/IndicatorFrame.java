package artskif.trader.indicator;

import artskif.trader.candle.CandlePeriod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record IndicatorFrame(Instant bucket,
                             CandlePeriod candleType,
                             Map<String, BigDecimal> values) {
}
