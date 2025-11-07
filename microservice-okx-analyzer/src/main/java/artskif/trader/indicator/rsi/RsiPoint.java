package artskif.trader.indicator.rsi;

import artskif.trader.candle.CandleTimeframe;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.Instant;

@RegisterForReflection
public record RsiPoint(
        Instant bucket,     // bucket (ms epoch)
        BigDecimal rsi,
        CandleTimeframe timeframe,
        Boolean pump,
        Boolean dump
) {
    public RsiPoint(Instant bucket, BigDecimal rsi, CandleTimeframe timeframe) {
        this(bucket, rsi, timeframe, null, null);
    }
}
