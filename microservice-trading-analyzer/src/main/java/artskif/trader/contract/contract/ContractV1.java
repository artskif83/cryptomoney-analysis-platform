package artskif.trader.contract.contract;

import artskif.trader.contract.ContractDataService;
import artskif.trader.contract.ContractRegistry;
import artskif.trader.contract.features.*;
import artskif.trader.contract.features.impl.ADXFeature;
import artskif.trader.contract.features.impl.BaseFeature;
import artskif.trader.contract.features.impl.RSIFeature;
import artskif.trader.contract.labels.impl.FutureReturnLabel;
import artskif.trader.entity.Contract;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Экземпляр контракта - описывает один отдельный контракт (одна запись в таблице contracts)
 * Отвечает за:
 * - Генерацию исторического набора фич и сохранение в таблицу features
 * - Генерацию фич для текущей свечи из liveBuffer
 * - Подписывание каждой строки фич специальным хешкодом контракта
 */
@ApplicationScoped
public class ContractV1 extends AbstractContract {

    private static final String NAME = "Test Contract-5m V1.0 ";


    // Конструктор без параметров для CDI proxy
    public ContractV1() {
        super(null, null);
    }

    @Inject
    public ContractV1(ContractDataService dataService, ContractRegistry registry) {
        super(dataService, registry);
    }

    /**
     * Создать и инициализировать контракт с метаданными
     *
     * @return инициализированный контракт с сохраненным хешем
     */
    @Override
    protected Contract initializeContract() {
        // Создаем контракт с метаданными
        Contract newContract = new Contract(NAME, "First testing contract 5m timeframe", "V1");

        // Добавляем фичи к контракту
        newContract.addMetadata(RSIFeature.getFeatureMetadata(
                Map.of(1, RSIFeature.RSIFeatureType.RSI_5M
                        , 2, RSIFeature.RSIFeatureType.RSI_5M_ON_4H),
                newContract
        ));

        newContract.addMetadata(ADXFeature.getFeatureMetadata(Map.of(
                1, ADXFeature.ADXFeatureType.ADX_5M
                , 2, ADXFeature.ADXFeatureType.ADX_5M_ON_4H), newContract));

        // Добавляем лейблы к контракту
        newContract.addMetadata(FutureReturnLabel.getLabelMetadata(100, newContract));

        // Генерируем и сохраняем hash
        newContract.contractHash = generateContractHash(newContract);
        dataService.saveContract(newContract);

        return newContract;
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    protected Feature getBaseFeature() {
        Feature baseFeature = registry.getFeature(BaseFeature.BaseFeatureType.BASE_5M.getName()).orElse(null);
        if (baseFeature == null) {
            Log.errorf("❌ Не удалось получить индикатор главной фичи для контракта %s. Пропуск генерации исторических фич.",
                    contract.name);
            return null;
        }
        return baseFeature;
    }

}

