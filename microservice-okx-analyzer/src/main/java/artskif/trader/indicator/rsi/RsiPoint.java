package artskif.trader.indicator.rsi;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@RegisterForReflection
public class RsiPoint {
    private Instant timestamp;     // bucket (ms epoch)
    private BigDecimal rsi;

    // конструктор по умолчанию (с пустыми значениями)
    public RsiPoint() {
        this.timestamp = null;
        this.rsi = null;
    }

    public RsiPoint(Instant ts, BigDecimal rsi) {
        this.timestamp = ts;
        this.rsi = rsi;
    }

    // фабричный метод "пустой точки"
    public static RsiPoint empty() {
        return new RsiPoint();
    }
}
