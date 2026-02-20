package artskif.trader.entity;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.event.common.Direction;
import artskif.trader.strategy.event.common.TradeEventType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Сущность для хранения торговых событий (TradeEvent) в БД
 * Используется для отображения событий на графике в Grafana
 */
@Entity
@Table(name = "trade_events")
public class TradeEventEntity extends PanacheEntityBase {

    @EmbeddedId
    public TradeEventId id;

    @Column(name = "uuid", updatable = false, nullable = false)
    public UUID uuid;

    @Column(name = "event_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    public TradeEventType eventType;

    @Column(name = "direction", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    public Direction direction;

    @Column(name = "instrument", nullable = false, length = 50)
    public String instrument;

    @Column(name = "event_price", nullable = false, precision = 18, scale = 8)
    public BigDecimal eventPrice;

    @Column(name = "stop_loss_percentage", precision = 10, scale = 4)
    public BigDecimal stopLossPercentage;

    @Column(name = "take_profit_percentage", precision = 10, scale = 4)
    public BigDecimal takeProfitPercentage;

    @Column(name = "is_test", nullable = false)
    public Boolean isTest;

    public TradeEventEntity() {
    }

    public TradeEventEntity(
            TradeEventType eventType,
            Direction direction,
            String instrument,
            BigDecimal eventPrice,
            BigDecimal stopLossPercentage,
            BigDecimal takeProfitPercentage,
            CandleTimeframe timeframe,
            String tag,
            Instant timestamp,
            Boolean isTest
    ) {
        this.id = new TradeEventId(timeframe, tag, timestamp);
        this.uuid = UUID.randomUUID();
        this.eventType = eventType;
        this.direction = direction;
        this.instrument = instrument;
        this.eventPrice = eventPrice;
        this.stopLossPercentage = stopLossPercentage;
        this.takeProfitPercentage = takeProfitPercentage;
        this.isTest = isTest;
    }

    /**
     * Составной первичный ключ для TradeEventEntity
     */
    @Embeddable
    public static class TradeEventId implements java.io.Serializable {

        @Column(name = "timeframe", nullable = false, length = 20)
        public String timeframe;

        @Column(name = "tag", nullable = false)
        public String tag;

        @Column(name = "timestamp", nullable = false)
        public Instant timestamp;

        public TradeEventId() {
        }

        public TradeEventId(CandleTimeframe timeframe, String tag, Instant timestamp) {
            this.timeframe = timeframe.getShortName();
            this.tag = tag;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TradeEventId that = (TradeEventId) o;
            return java.util.Objects.equals(timeframe, that.timeframe) &&
                   java.util.Objects.equals(tag, that.tag) &&
                   java.util.Objects.equals(timestamp, that.timestamp);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(timeframe, tag, timestamp);
        }
    }
}
