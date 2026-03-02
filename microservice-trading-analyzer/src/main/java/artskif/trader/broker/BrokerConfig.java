package artskif.trader.broker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

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
     * Коэффициент деления депозита для расчёта максимально допустимого убытка
     * до стоп-лосса (riskPerTrade = deposit / coefficient).
     * По умолчанию: 40
     */
    @Inject
    @ConfigProperty(name = "broker.deposit-risk-divisor", defaultValue = "40")
    int depositRiskDivisor;

    /**
     * Минимальный интервал между ставками.
     * По умолчанию: 1 час (PT1H)
     */
    @Inject
    @ConfigProperty(name = "broker.bet-interval", defaultValue = "PT1H")
    Duration betInterval;

    /**
     * Минимальное количество минут ожидания перед открытием следующей позиции.
     * По умолчанию: 60 минут
     */
    @Inject
    @ConfigProperty(name = "broker.minutes-between-positions", defaultValue = "60")
    int minutesBetweenPositions;

    /**
     * Максимальное количество позиций, которые можно открыть за последние 24 часа.
     * По умолчанию: 5
     */
    @Inject
    @ConfigProperty(name = "broker.max-positions-per-day", defaultValue = "5")
    int maxPositionsPerDay;

    public double getOrderCancelDistancePercent() {
        return orderCancelDistancePercent;
    }

    public int getDepositRiskDivisor() {
        return depositRiskDivisor;
    }

    public Duration getBetInterval() {
        return betInterval;
    }

    public int getMinutesBetweenPositions() {
        return minutesBetweenPositions;
    }

    public int getMaxPositionsPerDay() {
        return maxPositionsPerDay;
    }
}
