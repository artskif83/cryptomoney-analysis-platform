package artskif.trader.contract;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.contract.contract.AbstractContract;
import artskif.trader.contract.features.ContractFeatureRegistry;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для управления контрактами и их фичами
 */
@ApplicationScoped
public class ContractService {

    ContractFeatureRegistry featureRegistry;
    private final Map<String, AbstractContract> contractMap = new HashMap<>();

    @Inject
    public ContractService(ContractFeatureRegistry featureRegistry, Instance<AbstractContract> contractInstances) {
        this.featureRegistry = featureRegistry;
        contractInstances.forEach(contract -> {
            String contractName = contract.getName();
            contractMap.put(contractName, contract);
            Log.infof("📝 Зарегистрирован контракт: %s", contractName);
        });
    }

    /**
     * Сгенерировать исторические фичи для всех контрактов
     */
    public void generateHistoricalFeaturesForAll(CandleTimeframe timeframe) {
        Log.info("📊 Начало генерации исторических фич для всех контрактов");

        contractMap.values().forEach(instance -> {
            try {
                instance.generateHistoricalFeatures(timeframe);
            } catch (Exception e) {
                Log.errorf(e, "❌ Ошибка при генерации исторических фич для контракта: %s",
                          instance.getName());
            }
        });

        Log.info("✅ Завершена генерация исторических фич для всех контрактов");
    }

    /**
     * Сгенерировать исторические фичи для конкретного контракта
     *
     * @param contractName имя контракта
     * @param timeframe таймфрейм для генерации
     * @return true если контракт найден и фичи сгенерированы, false если контракт не найден
     */
    public boolean generateHistoricalFeaturesForContract(String contractName, CandleTimeframe timeframe) {
        Log.infof("📊 Генерация исторических фич для контракта: %s, таймфрейм: %s",
                  contractName, timeframe);

        AbstractContract contract = contractMap.get(contractName);

        if (contract == null) {
            Log.warnf("⚠️ Контракт не найден: %s", contractName);
            return false;
        }

        try {
            contract.generateHistoricalFeatures(timeframe);
            Log.infof("✅ Исторические фичи сгенерированы для контракта: %s", contractName);
            return true;
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при генерации исторических фич для контракта: %s", contractName);
            throw new RuntimeException("Ошибка при генерации фич: " + e.getMessage(), e);
        }
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
}

