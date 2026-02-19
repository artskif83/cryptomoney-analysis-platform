package artskif.trader.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Embeddable
public class WideCandleId implements Serializable {

    @Column(length = 10)
    public String tf;

    @Column
    public String tag;

    @Column(columnDefinition = "TIMESTAMP") // ⚡ без таймзоны
    public Instant ts;

    public WideCandleId() {}

    public WideCandleId(String tf, String tag, Instant ts) {
        this.tf = tf;
        this.tag = tag;
        this.ts = ts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WideCandleId)) return false;
        WideCandleId other = (WideCandleId) o;
        return Objects.equals(tf, other.tf)
                && Objects.equals(tag, other.tag)
                && Objects.equals(ts, other.ts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tf, tag, ts);
    }
}
