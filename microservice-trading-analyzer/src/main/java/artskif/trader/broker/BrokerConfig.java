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


    public boolean isTradingEnabled() {
        return tradingEnabled;
    }

    public boolean isAllStrategiesEnabled() {
        return allStrategiesEnabled;
    }
}
