package artskif.trader.broker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Конфигурация брокерского модуля.
 */
@ApplicationScoped
public class BrokerConfig {

    /**
     * Расстояние до ордера (в процентах), при котором ордер может быть отменён.
     * По умолчанию: 0.3%
     */
    @Inject
    @ConfigProperty(name = "broker.order-cancel-distance-percent", defaultValue = "0.3")
    double orderCancelDistancePercent;

    /**
     * Минимальное расстояние (в процентах) от текущей eventPrice до цены открытия существующей позиции,
     * при котором противоположная позиция считается «достаточно далёкой» для закрытия и переоткрытия.
     * По умолчанию: 1.0%
     */
    @Inject
    @ConfigProperty(name = "broker.min-position-distance-percent", defaultValue = "1.0")
    double minPositionDistancePercent;

    /**
     * Процент от депозита, используемый для расчёта максимально допустимого убытка
     * до стоп-лосса (riskPerTrade = deposit * depositRiskPercent / 100).
     * Например, значение 5 означает 5% от депозита на ставку.
     * По умолчанию: 2.5
     */
    @Inject
    @ConfigProperty(name = "broker.deposit-risk-percent", defaultValue = "2.5")
    double depositRiskPercent;

    /**
     * Минимальное количество минут ожидания перед открытием следующей позиции.
     * По умолчанию: 60 минут
     */
    @Inject
    @ConfigProperty(name = "broker.minutes-between-positions", defaultValue = "60")
    int minutesBetweenPositions;

    /**
     * Максимальное количество убыточных позиций, которые можно получить за последние 24 часа.
     * При достижении лимита новые позиции не открываются до следующего дня.
     * По умолчанию: 3
     */
    @Inject
    @ConfigProperty(name = "broker.max-losing-positions-per-day", defaultValue = "3")
    int maxLosingPositionsPerDay;

    /**
     * Флаг, разрешающий открытие позиций (торговлю).
     * Если false — метод openPosition не вызывается и никакие покупки не осуществляются.
     * По умолчанию: true
     */
    @Inject
    @ConfigProperty(name = "broker.trading-enabled", defaultValue = "true")
    boolean tradingEnabled;

    /**
     * Глобальный флаг, разрешающий запуск всех стратегий.
     * Если false — ни одна стратегия не запустится, независимо от значения isEnabled() каждой из них.
     * По умолчанию: true
     */
    @Inject
    @ConfigProperty(name = "strategy.all-enabled", defaultValue = "true")
    boolean allStrategiesEnabled;

    public double getOrderCancelDistancePercent() {
        return orderCancelDistancePercent;
    }

    public double getMinPositionDistancePercent() {
        return minPositionDistancePercent;
    }

    public double getDepositRiskPercent() {
        return depositRiskPercent;
    }

    public int getMinutesBetweenPositions() {
        return minutesBetweenPositions;
    }

    public int getMaxLosingPositionsPerDay() {
        return maxLosingPositionsPerDay;
    }

    public boolean isTradingEnabled() {
        return tradingEnabled;
    }

    public boolean isAllStrategiesEnabled() {
        return allStrategiesEnabled;
    }
}
