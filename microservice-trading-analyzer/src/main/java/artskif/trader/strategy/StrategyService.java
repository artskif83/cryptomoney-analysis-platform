package artskif.trader.strategy;

import artskif.trader.strategy.database.ColumnsRegistry;
import artskif.trader.strategy.database.schema.AbstractSchema;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления стратегиями и их жизненным циклом
 */
@ApplicationScoped
public class StrategyService {

    ColumnsRegistry registry;
    private final Map<String, AbstractSchema> contractMap = new HashMap<>();
    private final Map<String, AbstractStrategy> strategyMap = new ConcurrentHashMap<>();

    @Inject
    public StrategyService(ColumnsRegistry registry,
                           Instance<AbstractSchema> contractInstances,
                           Instance<AbstractStrategy> strategyInstances) {
        this.registry = registry;

        // Регистрируем схему
        contractInstances.forEach(contract -> {
            String contractName = contract.getName();
            contractMap.put(contractName, contract);
            Log.infof("📋 Зарегистрирована схема: %s", contractName);
        });

        // Регистрируем стратегии
        strategyInstances.forEach(strategy -> {
            String strategyName = strategy.getName();
            strategyMap.put(strategyName, strategy);
            Log.infof("📋 Зарегистрирована стратегия: %s", strategyName);
        });
    }

    /**
     * Получить список всех зарегистрированных стратегий
     *
     * @return карта имен стратегий и их статусов запуска
     */
    public Map<String, Boolean> getAllStrategies() {
        Map<String, Boolean> result = new HashMap<>();
        strategyMap.forEach((name, strategy) ->
                result.put(name, strategy.isEnabled())
        );
        return result;
    }

    /**
     * Проверить, запущена ли стратегия
     *
     * @param strategyName имя стратегии
     * @return true если стратегия запущена
     */
    public boolean isStrategyRunning(String strategyName) {
        AbstractStrategy strategy = strategyMap.get(strategyName);
        return strategy != null && strategy.isEnabled();
    }

    /**
     * Получить имя контракта по его ID из базы данных
     *
     * @param contractId ID контракта
     * @return имя контракта или null если не найден
     */
    public String getContractNameById(Long contractId) {
        artskif.trader.entity.Contract contract = artskif.trader.entity.Contract.findById(contractId);
        return contract != null ? contract.name : null;
    }

    /**
     * Запустить бэктест для стратегии по имени
     *
     * @param strategyName имя стратегии
     * @param startIndex   индекс бара, с которого начать бэктест (опционально)
     * @param endIndex     индекс бара, которым закончить бэктест (опционально)
     * @return true если бэктест успешно запущен
     */
    public boolean runBacktest(String strategyName, Integer startIndex, Integer endIndex) {
        AbstractStrategy strategy = strategyMap.get(strategyName);

        if (strategy == null) {
            Log.warnf("⚠️ Стратегия не найдена: %s", strategyName);
            return false;
        }

        try {
            Log.infof("📊 Запуск бэктеста для стратегии: %s", strategyName);
            strategy.backtest(startIndex, endIndex);
            Log.infof("✅ Бэктест завершен для стратегии: %s", strategyName);
            return true;

        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при выполнении бэктеста для стратегии: %s", strategyName);
            return false;
        }
    }
}

