package artskif.trader.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Embeddable
public class CandleId implements Serializable {
    @Column(nullable = false)
    public String symbol;

    @Column(nullable = false)
    public String tf;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    public OffsetDateTime ts; // начало свечи (UTC)

    public CandleId() {}
    public CandleId(String symbol, String tf, OffsetDateTime ts) {
        this.symbol = symbol; this.tf = tf; this.ts = ts;
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CandleId)) return false;
        CandleId that = (CandleId) o;
        return Objects.equals(symbol, that.symbol)
                && Objects.equals(tf, that.tf)
                && Objects.equals(ts, that.ts);
    }
    @Override public int hashCode() { return Objects.hash(symbol, tf, ts); }
}
