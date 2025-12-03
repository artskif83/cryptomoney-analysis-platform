package artskif.trader.dto;

import artskif.trader.candle.CandleTimeframe;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@RegisterForReflection
public class RsiPointDto {
    private Instant bucket;     // bucket (ms epoch)
    private BigDecimal rsi;
    private CandleTimeframe timeframe;
    private Boolean pump;
    private Boolean dump;
    private String instrument;
    private Boolean saved;

    public RsiPointDto(Instant bucket, BigDecimal rsi, CandleTimeframe timeframe, String instrument) {
        this.bucket = bucket;
        this.rsi = rsi;
        this.timeframe = timeframe;
        this.pump = null;
        this.dump = null;
        this.instrument = instrument;
        this.saved = false;
    }
}
