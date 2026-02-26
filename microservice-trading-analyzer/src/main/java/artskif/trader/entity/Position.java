package artskif.trader.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Сущность для хранения открытых позиций из OKX.
 *
 * Сейчас реализована как "аналог PendingOrder" по набору полей, чтобы можно было
 * сохранить позиции так же, как и ожидающие ордера.
 *
 * Важно: OKX для позиций обычно возвращает собственные поля (posId, avgPx, upl и т.д.).
 * Когда будем подключать сохранение позиций, вероятно, стоит уточнить контракт и PK.
 */
@Entity
@Table(name = "positions")
public class Position extends PanacheEntityBase {

    /**
     * Идентификатор позиции на бирже.
     *
     * По аналогии с PendingOrder используем строковый ID как первичный ключ.
     * Если в данных позиции отсутствует стабильный ID, можно будет заменить на surrogate key
     * или на составной ключ.
     */
    @Id
    @Column(name = "pos_id", nullable = false, length = 128)
    public String posId;

    /**
     * Клиентский ID (если используется/передается).
     */
    @Column(name = "cl_ord_id", length = 128)
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
     * Цена (для позиции это может быть avgPx/markPx и т.п., пока поле зеркалит PendingOrder.px)
     */
    @Column(name = "px", precision = 18, scale = 8)
    public BigDecimal px;

    /**
     * Размер позиции (sz)
     */
    @Column(name = "sz", precision = 18, scale = 8)
    public BigDecimal sz;

    /**
     * Направление позиции: long/short/net(если не удалось определить).
     */
    @Column(name = "pos_side", length = 10)
    public String posSide;

    /**
     * Режим торговли (tdMode): cross, isolated, cash
     */
    @Column(name = "td_mode", length = 20)
    public String tdMode;

    /**
     * Плечо (leverage)
     */
    @Column(name = "lever", precision = 5, scale = 2)
    public BigDecimal lever;

    /**
     * Состояние позиции.
     * Для синхронизации с биржей позиции, которых нет в очередном снимке, помечаем как CLOSED.
     */
    @Column(name = "state", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    public OrderState state;

    /**
     * Время создания позиции/события в источнике (если доступно)
     */
    @Column(name = "c_time")
    public Instant cTime;

    /**
     * Время обновления позиции/события в источнике (если доступно)
     */
    @Column(name = "u_time")
    public Instant uTime;

    /**
     * Временная метка создания записи в нашей БД
     */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    /**
     * Временная метка последнего обновления записи в нашей БД
     */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    public Position() {
    }

    public Position(
            String posId,
            String clOrdId,
            String instId,
            String instType,
            BigDecimal px,
            BigDecimal sz,
            String posSide,
            String tdMode,
            BigDecimal lever,
            Instant cTime,
            Instant uTime
    ) {
        this.posId = posId;
        this.clOrdId = clOrdId;
        this.instId = instId;
        this.instType = instType;
        this.px = px;
        this.sz = sz;
        this.posSide = posSide;
        this.tdMode = tdMode;
        this.lever = lever;
        this.cTime = cTime;
        this.uTime = uTime;
        this.state = OrderState.LIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "Position{" +
                "posId='" + posId + '\'' +
                ", clOrdId='" + clOrdId + '\'' +
                ", instId='" + instId + '\'' +
                ", instType='" + instType + '\'' +
                ", px=" + px +
                ", sz=" + sz +
                ", side='" + posSide + '\'' +
                ", tdMode='" + tdMode + '\'' +
                ", lever=" + lever +
                ", state='" + state + '\'' +
                ", cTime=" + cTime +
                ", uTime=" + uTime +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
