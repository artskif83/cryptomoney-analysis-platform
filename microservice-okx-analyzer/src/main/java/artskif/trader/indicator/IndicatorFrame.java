package artskif.trader.indicator;

import artskif.trader.candle.CandleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record IndicatorFrame(Instant bucket,
                             CandleType candleType,
                             Map<String, BigDecimal> values) {
}
