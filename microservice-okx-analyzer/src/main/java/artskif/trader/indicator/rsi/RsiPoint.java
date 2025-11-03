package artskif.trader.indicator.rsi;

import artskif.trader.candle.CandleTimeframe;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@RegisterForReflection
public class RsiPoint {
    private Instant bucket;     // bucket (ms epoch)
    private BigDecimal rsi;
    private CandleTimeframe timeframe;

    // конструктор по умолчанию (с пустыми значениями)
    public RsiPoint() {
        this.bucket = null;
        this.rsi = null;
        this.timeframe = null;
    }

    public RsiPoint(Instant ts, BigDecimal rsi, CandleTimeframe timeframe) {
        this.bucket = ts;
        this.rsi = rsi;
        this.timeframe = timeframe;
    }

    // фабричный метод "пустой точки"
    public static RsiPoint empty() {
        return new RsiPoint();
    }
}
