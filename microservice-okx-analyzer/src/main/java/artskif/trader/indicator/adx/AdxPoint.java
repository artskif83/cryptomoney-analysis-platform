package artskif.trader.indicator.adx;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@RegisterForReflection
public class AdxPoint {
    private Instant timestamp;     // bucket (ms epoch)
    private BigDecimal plusDI;  // +DI
    private BigDecimal minusDI; // -DI
    private BigDecimal adx;
    private BigDecimal dx;

    // конструктор по умолчанию (с пустыми значениями)
    public AdxPoint() {
        this.timestamp = null;
        this.plusDI = null;
        this.minusDI = null;
        this.adx = null;
    }

    public AdxPoint(Instant ts, BigDecimal plusDI, BigDecimal minusDI, BigDecimal dx, BigDecimal adx) {
        this.timestamp = ts;
        this.plusDI = plusDI;
        this.minusDI = minusDI;
        this.adx = adx;
        this.dx = dx;
    }

    // фабричный метод "пустой точки"
    public static AdxPoint empty() {
        return new AdxPoint();
    }
}
