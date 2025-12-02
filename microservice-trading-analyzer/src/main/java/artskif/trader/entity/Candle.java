package artskif.trader.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "candles")
public class Candle extends PanacheEntityBase {

    @EmbeddedId
    public CandleId id;

    @Column(nullable = false, precision = 18, scale = 8)
    public BigDecimal open;

    @Column(nullable = false, precision = 18, scale = 8)
    public BigDecimal high;

    @Column(nullable = false, precision = 18, scale = 8)
    public BigDecimal low;

    @Column(nullable = false, precision = 18, scale = 8)
    public BigDecimal close;

    @Column(nullable = true, precision = 30, scale = 8)
    public BigDecimal volume;

    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean confirmed;

    public Candle() {
    }

    public Candle(CandleId id, BigDecimal open, BigDecimal high,
                  BigDecimal low, BigDecimal close, BigDecimal volume,boolean confirmed) {
        this.id = id;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.confirmed = confirmed;
    }
}
