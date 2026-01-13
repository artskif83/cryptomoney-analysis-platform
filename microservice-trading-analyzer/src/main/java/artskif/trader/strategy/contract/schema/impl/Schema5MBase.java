package artskif.trader.strategy.contract.schema.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.contract.ContractDataService;
import artskif.trader.strategy.contract.ContractRegistry;
import artskif.trader.strategy.contract.schema.AbstractSchema;
import artskif.trader.strategy.contract.features.Feature;
import artskif.trader.strategy.contract.features.impl.ADXFeature;
import artskif.trader.strategy.contract.features.impl.CloseFeature;
import artskif.trader.strategy.contract.features.impl.RSIFeature;
import artskif.trader.strategy.contract.labels.impl.FutureReturnLabel;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Экземпляр контракта - описывает один отдельный контракт (одна запись в таблице contracts)
 * Отвечает за:
 * - Генерацию исторического набора фич и сохранение в таблицу features
 * - Генерацию фич для текущей свечи из liveBuffer
 * - Подписывание каждой строки фич специальным хешкодом контракта
 */
@ApplicationScoped
public class Schema5MBase extends AbstractSchema {

    private static final String NAME = "Test Contract-5m V1.0 ";


    // Конструктор без параметров для CDI proxy
    public Schema5MBase() {
        super(null, null);
    }

    @Inject
    public Schema5MBase(ContractDataService dataService, ContractRegistry registry) {
        super(dataService, registry);
    }

    /**
     * Создать и инициализировать контракт с метаданными
     */
    @PostConstruct
    public void initContract() {

        // Сначала проверяем, существует ли контракт
        Contract existingContract = dataService.findContractByName(NAME);
        if (existingContract != null) {
            Log.infof("📋 Контракт '%s' уже существует в БД (id: %d), используем существующий", NAME, existingContract.id);
            this.contract = existingContract;
            this.contractHash = existingContract.contractHash;
            return;
        }

        // Создаем контракт с метаданными
        Contract newContract = new Contract(NAME, "First testing contract 5m timeframe", "V1");

        // Добавляем все фичи к контракту одним вызовом
        List<ContractMetadata> allMetadata = new ArrayList<>();
        allMetadata.addAll(RSIFeature.getFeatureMetadata(
                Map.of(1, RSIFeature.RSIFeatureType.RSI_5M
                        , 2, RSIFeature.RSIFeatureType.RSI_5M_ON_4H),
                newContract
        ));
        allMetadata.addAll(ADXFeature.getFeatureMetadata(
                Map.of(3, ADXFeature.ADXFeatureType.ADX_5M
                        , 4, ADXFeature.ADXFeatureType.ADX_5M_ON_4H),
                newContract
        ));
        allMetadata.add(FutureReturnLabel.getLabelMetadata(100, newContract));
        newContract.addMetadata(allMetadata);

        // Генерируем и сохраняем hash
        newContract.contractHash = generateContractHash(newContract);
        this.contract = dataService.saveNewContract(newContract);
        this.contractHash = this.contract.contractHash;
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_5M;
    }

}

