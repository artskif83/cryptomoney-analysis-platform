package artskif.trader.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Сущность для хранения активных (ожидающих) ордеров из OKX
 * Соответствует данным из API OKX /api/v5/trade/orders-pending
 */
@Entity
@Table(name = "pending_orders")
public class PendingOrder extends PanacheEntityBase {

    /**
     * Клиентский ID ордера (clOrdId) - используется как первичный ключ
     */
    @Id
    @Column(name = "cl_ord_id", nullable = false, length = 128)
    public String clOrdId;

    /**
     * ID инструмента (например, BTC-USDT-SWAP)
     */
    @Column(name = "inst_id", nullable = false, length = 50)
    public String instId;

    /**
     * Тип инструмента (например, SWAP)
     */
    @Column(name = "inst_type", nullable = false, length = 20)
    public String instType;

    /**
     * Цена ордера (лимитная цена)
     */
    @Column(name = "px", nullable = false, precision = 18, scale = 8)
    public BigDecimal px;

    /**
     * Размер позиции ордера (в контрактах или базовой валюте)
     */
    @Column(name = "sz", nullable = false, precision = 18, scale = 8)
    public BigDecimal sz;

    /**
     * Направление (side): buy или sell
     * buy для long, sell для short
     */
    @Column(name = "side", nullable = false, length = 10)
    public String side;

    /**
     * Режим торговли (tdMode): cross, isolated, cash
     */
    @Column(name = "td_mode", nullable = false, length = 20)
    public String tdMode;

    /**
     * Плечо (leverage)
     */
    @Column(name = "lever", precision = 5, scale = 2)
    public BigDecimal lever;

    /**
     * Временная метка создания записи
     */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    /**
     * Временная метка последнего обновления
     */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    /**
     * ID ордера на бирже (ordId) - для справки
     */
    @Column(name = "ord_id", length = 128)
    public String ordId;

    /**
     * Состояние ордера: LIVE, PARTIALLY_FILLED, CLOSED
     */
    @Column(name = "state", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    public OrderState state;

    /**
     * Тип ордера (ordType): limit, market, post_only, fok, ioc
     */
    @Column(name = "ord_type", length = 20)
    public String ordType;

    public PendingOrder() {
    }

    public PendingOrder(
            String clOrdId,
            String instId,
            String instType,
            BigDecimal px,
            BigDecimal sz,
            String side,
            String tdMode,
            BigDecimal lever
    ) {
        this.clOrdId = clOrdId;
        this.instId = instId;
        this.instType = instType;
        this.px = px;
        this.sz = sz;
        this.side = side;
        this.tdMode = tdMode;
        this.lever = lever;
        this.state = OrderState.LIVE; // По умолчанию активный
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Обновить временную метку изменения
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "PendingOrder{" +
                "clOrdId='" + clOrdId + '\'' +
                ", instId='" + instId + '\'' +
                ", instType='" + instType + '\'' +
                ", px=" + px +
                ", sz=" + sz +
                ", side='" + side + '\'' +
                ", tdMode='" + tdMode + '\'' +
                ", lever=" + lever +
                ", state='" + state + '\'' +
                ", ordType='" + ordType + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
