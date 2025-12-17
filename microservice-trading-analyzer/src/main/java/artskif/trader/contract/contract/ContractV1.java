package artskif.trader.contract.contract;

import artskif.trader.contract.ContractDataService;
import artskif.trader.contract.ContractFeatureRegistry;
import artskif.trader.contract.FeatureRow;
import artskif.trader.contract.features.BaseFeature;
import artskif.trader.contract.features.Feature;
import artskif.trader.contract.features.RsiFeature;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractFeatureMetadata;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Экземпляр контракта - описывает один отдельный контракт (одна запись в таблице contracts)
 * Отвечает за:
 * - Генерацию исторического набора фич и сохранение в таблицу features
 * - Генерацию фич для текущей свечи из liveBuffer
 * - Подписывание каждой строки фич специальным хешкодом контракта
 */
@ApplicationScoped
public class ContractV1 extends AbstractContract {

    private static final String NAME = "Contract V1.0 ";

    private Contract contract;
    private String contractHash;

    // Конструктор без параметров для CDI proxy
    public ContractV1() {
        super(null, null);
    }

    @Inject
    public ContractV1(ContractDataService dataService, ContractFeatureRegistry featureRegistry) {
        super(dataService, featureRegistry);
    }

    /**
     * Создать и инициализировать контракт с метаданными
     * @return инициализированный контракт с сохраненным хешем
     */
    private Contract initializeContract() {
        // Создаем контракт с метаданными
        Contract newContract = new Contract(NAME, "Dummy Contract", "V1");

        // Добавляем фичи к контракту
        newContract.addFeature(RsiFeature.getFeatureMetadata(2, newContract));

        // Генерируем и сохраняем hash
        newContract.contractHash = generateContractHash(newContract);
        dataService.saveContract(newContract);

        return newContract;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Сгенерировать исторические фичи и сохранить в таблицу features
     * Это используется для обучения модели ML
     */
    @Override
    public void generateHistoricalFeatures() {
        // Инициализируем контракт
        Contract initializedContract = initializeContract();
        this.contract = initializedContract;
        this.contractHash = initializedContract.contractHash;

        Log.infof("📋 Contract: %s (id: %d, hash: %s)", contract.name, contract.id, contractHash);

        // Убеждаемся что контракт инициализирован перед генерацией фич
        if (contract == null) {
            Log.error("❌ Контракт не инициализирован. Контракт должен быть инициализирован в конструкторе перед генерацией фич.");
            return;
        }

        Log.infof("📊 Начало генерации исторических фич для контракта: %s", contract.name);

        // Проверка что колонки существуют
        for (ContractFeatureMetadata featureMetadata : contract.features) {
            dataService.ensureColumnExist(featureMetadata.featureName);
        }

        Feature baseFeature = featureRegistry.getFeature(BaseFeature.FEATURE_NAME).orElse(null);
        if (baseFeature == null) {
            Log.errorf("❌ Не удалось получить индикатор главной фичи для контракта %s. Пропуск генерации исторических фич.",
                    contract.name);
            return;
        }

        int processedCount = 0;
        List<FeatureRow> futureRows = new ArrayList<>();
        List<CandlestickDto> candlestickDtos = baseFeature.getCandlestickDtos();

        for (int i = 0; i < candlestickDtos.size(); i++) {
            FeatureRow featureRow = generateFeatureRow(candlestickDtos.get(i), contract.features, i);

            futureRows.add(featureRow);
            processedCount++;
        }

        // Сохраняем в БД
        dataService.saveFeatureRowsBatch(futureRows);

        Log.infof("✅ Завершена генерация исторических фич для контракта: %s. Обработано %d свечей",
                contract.name, processedCount);

    }

    private FeatureRow generateFeatureRow(CandlestickDto currentCandle, List<ContractFeatureMetadata> featureMetadatas, int index) {
        FeatureRow row = new FeatureRow(
                currentCandle.getInstrument(),
                currentCandle.getPeriod(),
                currentCandle.getTimestamp(),
                contractHash
        );

        // Добавляем базовые данные свечи
        row.addFeature("open", currentCandle.getOpen());
        row.addFeature("high", currentCandle.getHigh());
        row.addFeature("low", currentCandle.getLow());
        row.addFeature("close", currentCandle.getClose());
        row.addFeature("volume", currentCandle.getVolume());

        for (ContractFeatureMetadata featureMetadata : featureMetadatas) {
            try {


                // Вычисляем значение фичи
                Feature feature = featureRegistry.getFeature(featureMetadata.featureName).orElse(null);

                if (feature != null) {
                    row.addFeature(featureMetadata.featureName, feature.getIndicator().getValue(index).bigDecimalValue());
                } else {
                    Log.debugf("⚠️ Фича %s не существует в реестре для фич",
                            featureMetadata.featureName);
                }

            } catch (Exception e) {
                Log.errorf(e, "❌ Ошибка при вычислении фичи %s для свечи %s",
                        featureMetadata.featureName, currentCandle.getTimestamp());
            }
        }

        return row;
    }

}

