package artskif.trader.strategy.contract.contract.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.ContractDataService;
import artskif.trader.strategy.contract.ContractRegistry;
import artskif.trader.strategy.contract.contract.AbstractContract;
import artskif.trader.strategy.contract.features.Feature;
import artskif.trader.strategy.contract.features.impl.ADXFeature;
import artskif.trader.strategy.contract.features.impl.BaseFeature;
import artskif.trader.strategy.contract.features.impl.RSIFeature;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class Contract4HBase  extends AbstractContract {

    private static final String NAME = "Test Contract-4h V1.0 ";


    // Конструктор без параметров для CDI proxy
    public Contract4HBase() {
        super(null, null);
    }

    @Inject
    public Contract4HBase(ContractDataService dataService, ContractRegistry registry) {
        super(dataService, registry);
    }

    /**
     * Создать и инициализировать контракт с метаданными
     *
     * @return инициализированный контракт с сохраненным хешем
     */
    @Override
    protected Contract initializeContract() {
        // Сначала проверяем, существует ли контракт
        Contract existingContract = dataService.findContractByName(NAME);
        if (existingContract != null) {
            Log.infof("📋 Контракт '%s' уже существует в БД (id: %d), используем существующий", NAME, existingContract.id);
            return existingContract;
        }

        // Создаем новый контракт с метаданными
        Contract newContract = new Contract(NAME, "First testing contract 4h timeframe", "V1");

        // Добавляем все фичи к контракту одним вызовом
        List<ContractMetadata> allMetadata = new ArrayList<>();
        allMetadata.addAll(RSIFeature.getFeatureMetadata(
                Map.of(1, RSIFeature.RSIFeatureType.RSI_4H),
                newContract
        ));
        allMetadata.addAll(ADXFeature.getFeatureMetadata(
                Map.of(2, ADXFeature.ADXFeatureType.ADX_4H),
                newContract
        ));
        newContract.addMetadata(allMetadata);

        // Добавляем лейблы к контракту
//        newContract.addMetadata(FutureReturnLabel.getLabelMetadata(100, newContract));

        // Генерируем hash
        newContract.contractHash = generateContractHash(newContract);

        // Сохраняем новый контракт через транзакционный метод сервиса
        return dataService.saveNewContract(newContract);
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    protected Feature getBaseFeature() {
        Feature baseFeature = registry.getFeature(BaseFeature.BaseFeatureType.BASE_4H.getName()).orElse(null);
        if (baseFeature == null) {
            Log.errorf("❌ Не удалось получить индикатор главной фичи для контракта %s. Пропуск генерации исторических фич.",
                    contract.name);
            return null;
        }
        return baseFeature;
    }

    @Override
    protected CandleTimeframe getBaseTimeframe() {
        return CandleTimeframe.CANDLE_4H;
    }

}