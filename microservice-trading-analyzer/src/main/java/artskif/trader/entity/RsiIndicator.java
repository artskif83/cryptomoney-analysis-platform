package artskif.trader.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "indicators_rsi")
public class RsiIndicator extends PanacheEntityBase {

    @EmbeddedId
    public RsiIndicatorId id;

    @Column(name = "rsi_14", precision = 5, scale = 2)
    public BigDecimal rsi14;

    public RsiIndicator() {
    }

    public RsiIndicator(RsiIndicatorId id, BigDecimal rsi14) {
        this.id = id;
        this.rsi14 = rsi14;
    }
}

