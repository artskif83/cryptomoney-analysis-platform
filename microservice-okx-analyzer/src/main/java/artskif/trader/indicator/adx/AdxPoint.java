package artskif.trader.indicator.adx;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@RegisterForReflection
public class AdxPoint {
    private Instant timestamp;     // bucket (ms epoch)
    private BigDecimal adx;


    // конструктор по умолчанию (с пустыми значениями)
    public AdxPoint() {
        this.timestamp = null;
        this.adx = null;
    }

    public AdxPoint(Instant ts, BigDecimal adx) {
        this.timestamp = ts;
        this.adx = adx;
    }

    // фабричный метод "пустой точки"
    public static AdxPoint empty() {
        return new AdxPoint();
    }
}
