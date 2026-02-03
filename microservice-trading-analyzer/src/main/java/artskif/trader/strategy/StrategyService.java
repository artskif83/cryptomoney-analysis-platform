package artskif.trader.strategy;

import artskif.trader.events.candle.CandleEventBus;
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
    private final CandleEventBus eventBus;

    @Inject
    public StrategyService(ColumnsRegistry registry,
                           Instance<AbstractSchema> contractInstances,
                           Instance<AbstractStrategy> strategyInstances,
                           CandleEventBus eventBus) {
        this.registry = registry;
        this.eventBus = eventBus;

        // Регистрируем контракты
        contractInstances.forEach(contract -> {
            String contractName = contract.getName();
            contractMap.put(contractName, contract);
            Log.infof("📋 Зарегистрирован контракт: %s", contractName);
        });

        // Регистрируем стратегии
        strategyInstances.forEach(strategy -> {
            String strategyName = strategy.getName();
            strategyMap.put(strategyName, strategy);
            Log.infof("📋 Зарегистрирована стратегия: %s", strategyName);
        });
    }

    /**
     * Запустить стратегию по имени
     *
     * @param strategyName имя стратегии
     * @return true если стратегия успешно запущена
     */
    public boolean startStrategy(String strategyName) {
        AbstractStrategy strategy = strategyMap.get(strategyName);

        if (strategy == null) {
            Log.warnf("⚠️ Стратегия не найдена: %s", strategyName);
            return false;
        }

        if (strategy.isRunning()) {
            Log.warnf("⚠️ Стратегия уже запущена: %s", strategyName);
            return false;
        }

        try {
            // Помечаем стратегию как запущенную
            strategy.setRunning(true);
            // Подписываемся на события
            eventBus.subscribe(strategy);

            Log.infof("✅ Стратегия запущена: %s (таймфрейм: %s)",
                    strategyName, strategy.getTimeframe());
            return true;

        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при запуске стратегии: %s", strategyName);
            return false;
        }
    }

    /**
     * Остановить стратегию по имени
     *
     * @param strategyName имя стратегии
     * @return true если стратегия успешно остановлена
     */
    public boolean stopStrategy(String strategyName) {
        AbstractStrategy strategy = strategyMap.get(strategyName);

        if (strategy == null) {
            Log.warnf("⚠️ Стратегия не найдена: %s", strategyName);
            return false;
        }

        if (!strategy.isRunning()) {
            Log.warnf("⚠️ Стратегия не запущена: %s", strategyName);
            return false;
        }

        try {
            // Отписываемся от событий
            eventBus.unsubscribe(strategy);
            // Помечаем стратегию как остановленную
            strategy.setRunning(false);

            Log.infof("🛑 Стратегия остановлена: %s", strategyName);
            return true;

        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при остановке стратегии: %s", strategyName);
            return false;
        }
    }

    /**
     * Получить список всех зарегистрированных стратегий
     *
     * @return карта имен стратегий и их статусов запуска
     */
    public Map<String, Boolean> getAllStrategies() {
        Map<String, Boolean> result = new HashMap<>();
        strategyMap.forEach((name, strategy) ->
                result.put(name, strategy.isRunning())
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
        return strategy != null && strategy.isRunning();
    }

    /**
     * Сгенерировать исторические фичи для всех контрактов
     */
    public void generateHistoricalFeaturesForAll() {
        Log.info("📊 Начало генерации исторических фич для всех контрактов");

//        contractMap.values().forEach(instance -> {
//            try {
//                instance.generateHistoricalFeatures();
//            } catch (Exception e) {
//                Log.errorf(e, "❌ Ошибка при генерации исторических фич для всех контрактов. Текущий контракт: %s",
//                          instance.getName());
//            }
//        });

        Log.info("✅ Завершена генерация исторических фич для всех контрактов");
    }

    /**
     * Сгенерировать исторические фичи для конкретного контракта
     *
     * @param contractName имя контракта
     * @return true если контракт найден и фичи сгенерированы, false если контракт не найден
     */
    public boolean generateHistoricalFeaturesForContract(String contractName) {
        Log.infof("📊 Генерация исторических фич для контракта: %s",
                contractName);

//        AbstractSchema contract = contractMap.get(contractName);
//
//        if (contract == null) {
//            Log.warnf("⚠️ Контракт не найден: %s", contractName);
//            return false;
//        }
//
//        try {
//            contract.generateHistoricalFeatures();
//            Log.infof("✅ Исторические фичи сгенерированы для контракта: %s", contractName);
//            return true;
//        } catch (Exception e) {
//            Log.errorf(e, "❌ Ошибка при генерации исторических фич для контракта: %s", contractName);
//            throw new RuntimeException("Ошибка при генерации фич: " + e.getMessage(), e);
//        }
        return true;
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
     * Сгенерировать предсказание
     */
    public void generatePredict() {
        Log.debug("🔴 Получить текущее предсказание");
    }

    /**
     * Запустить бэктест для стратегии по имени
     *
     * @param strategyName имя стратегии
     * @return true если бэктест успешно запущен
     */
    public boolean runBacktest(String strategyName) {
        AbstractStrategy strategy = strategyMap.get(strategyName);

        if (strategy == null) {
            Log.warnf("⚠️ Стратегия не найдена: %s", strategyName);
            return false;
        }

        try {
            Log.infof("📊 Запуск бэктеста для стратегии: %s", strategyName);
            strategy.backtest();
            Log.infof("✅ Бэктест завершен для стратегии: %s", strategyName);
            return true;

        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при выполнении бэктеста для стратегии: %s", strategyName);
            return false;
        }
    }
}

