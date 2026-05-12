package artskif.trader.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Параметры для принятия решения о создании будущего ордера.
 * Хранит торговые настройки стратегии: силу тренда, риск, стоплос и т.д.
 */
@Entity
@Table(name = "order_creation_params")
public class OrderCreationParams extends PanacheEntityBase {

    /**
     * Суррогатный первичный ключ — генерируется БД.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    /**
     * Сила тренда — целочисленное значение от -100 до 100.
     */
    @Column(name = "trend_strength", nullable = false)
    public Integer trendStrength;

    /**
     * Процент депозита, которым рискуем при достижении стоплоса для LONG позиции.
     */
    @Column(name = "long_deposit_risk_percent", nullable = false, precision = 10, scale = 4)
    public BigDecimal longDepositRiskPercent;

    /**
     * Если true — для LONG позиции допускается только закрытие, увеличение запрещено.
     */
    @Column(name = "long_only_close", nullable = false)
    public boolean longOnlyClose;

    /**
     * Процент депозита, которым рискуем при достижении стоплоса для SHORT позиции.
     */
    @Column(name = "short_deposit_risk_percent", nullable = false, precision = 10, scale = 4)
    public BigDecimal shortDepositRiskPercent;

    /**
     * Если true — для SHORT позиции допускается только закрытие, увеличение запрещено.
     */
    @Column(name = "short_only_close", nullable = false)
    public boolean shortOnlyClose;

    /**
     * Процентное отклонение цены от текущей при срабатывании стоплоса.
     */
    @Column(name = "stop_loss_deviation_percent", nullable = false, precision = 10, scale = 4)
    public BigDecimal stopLossDeviationPercent;

    /**
     * Время ожидания до следующего события (в минутах).
     */
    @Column(name = "wait_minutes", nullable = false)
    public Integer waitMinutes;

    /**
     * Максимальный допустимый размер существующей позиции в процентах от депозита.
     */
    @Column(name = "max_position_size_percent", nullable = false, precision = 10, scale = 4)
    public BigDecimal maxPositionSizePercent;


    /**
     * Если true — закрывать противоположную LONG позицию при открытии SHORT.
     */
    @Column(name = "close_opposite_long", nullable = false)
    public boolean closeOppositeLong;

    /**
     * Если true — закрывать противоположную SHORT позицию при открытии LONG.
     */
    @Column(name = "close_opposite_short", nullable = false)
    public boolean closeOppositeShort;

    /**
     * Время создания записи.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public OrderCreationParams() {
    }

    public OrderCreationParams(Integer trendStrength,
                               BigDecimal longDepositRiskPercent,
                               boolean longOnlyClose,
                               BigDecimal shortDepositRiskPercent,
                               boolean shortOnlyClose,
                               BigDecimal stopLossDeviationPercent,
                               Integer waitMinutes,
                               BigDecimal maxPositionSizePercent,
                               boolean closeOppositeLong,
                               boolean closeOppositeShort) {
        this.trendStrength = trendStrength;
        this.longDepositRiskPercent = longDepositRiskPercent;
        this.longOnlyClose = longOnlyClose;
        this.shortDepositRiskPercent = shortDepositRiskPercent;
        this.shortOnlyClose = shortOnlyClose;
        this.stopLossDeviationPercent = stopLossDeviationPercent;
        this.waitMinutes = waitMinutes;
        this.maxPositionSizePercent = maxPositionSizePercent;
        this.closeOppositeLong = closeOppositeLong;
        this.closeOppositeShort = closeOppositeShort;
    }
}

