package artskif.trader.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Embeddable
public class RsiIndicatorId implements Serializable {

    @Column(nullable = false, length = 32)
    public String symbol;
    @Column(nullable = false, length = 10)
    public String tf;
    @Column(nullable = false, columnDefinition = "TIMESTAMP")
    public Instant ts;

    public RsiIndicatorId() {
    }

    public RsiIndicatorId(String symbol, String tf, Instant ts) {
        this.symbol = symbol;
        this.tf = tf;
        this.ts = ts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RsiIndicatorId)) return false;
        RsiIndicatorId other = (RsiIndicatorId) o;

        return Objects.equals(symbol, other.symbol)
                && Objects.equals(tf, other.tf)
                && Objects.equals(ts, other.ts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, tf, ts);
    }

}