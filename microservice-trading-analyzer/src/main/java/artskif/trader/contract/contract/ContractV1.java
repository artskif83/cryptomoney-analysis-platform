package artskif.trader.contract.contract;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.contract.ContractDataService;
import artskif.trader.contract.features.*;
import artskif.trader.contract.FeatureRow;
import artskif.trader.contract.labels.ContractLabelRegistry;
import artskif.trader.contract.labels.FutureReturnLabel;
import artskif.trader.contract.labels.Label;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.entity.MetadataType;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

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
public class ContractV1 extends AbstractContract {

    private static final String NAME = "Test Contract-5m V1.0 ";

    private Contract contract;
    private String contractHash;

    // Конструктор без параметров для CDI proxy
    public ContractV1() {
        super(null, null, null);
    }

    @Inject
    public ContractV1(ContractDataService dataService, ContractFeatureRegistry featureRegistry, ContractLabelRegistry labelRegistry) {
        super(dataService, featureRegistry, labelRegistry);
    }

    /**
     * Создать и инициализировать контракт с метаданными
     *
     * @return инициализированный контракт с сохраненным хешем
     */
    private Contract initializeContract() {
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

    /**
     * Сгенерировать исторические фичи и сохранить в таблицу features
     * Это используется для обучения модели ML
     */
    @Override
    public void generateHistoricalFeatures(CandleTimeframe timeframe) {
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
        for (ContractMetadata metadata : contract.metadata) {
            dataService.ensureColumnExist(metadata.name, metadata.metadataType);
        }

        Feature baseFeature = getBaseFeature();
        if (baseFeature == null) return;

        int processedCount = 0;
        List<FeatureRow> futureRows = new ArrayList<>();
        BarSeries barSeries = baseFeature.getIndicator(timeframe).getBarSeries();

        for (int i = 0; i < barSeries.getBarCount(); i++) {
            FeatureRow featureRow = generateFeatureRow(timeframe, barSeries.getBar(i), contract.metadata, i);

            futureRows.add(featureRow);
            processedCount++;
        }

        // Сохраняем в БД
        dataService.saveFeatureRowsBatch(futureRows);

        Log.infof("✅ Завершена генерация исторических фич для контракта: %s. Обработано %d свечей",
                contract.name, processedCount);

    }

    @Override
    protected Feature getBaseFeature() {
        Feature baseFeature = featureRegistry.getFeature(BaseFeature.BaseFeatureType.BASE_5M.getName()).orElse(null);
        if (baseFeature == null) {
            Log.errorf("❌ Не удалось получить индикатор главной фичи для контракта %s. Пропуск генерации исторических фич.",
                    contract.name);
            return null;
        }
        return baseFeature;
    }

    private FeatureRow generateFeatureRow(CandleTimeframe timeframe, Bar bar, List<ContractMetadata> metadatas, int index) {
        FeatureRow row = new FeatureRow(
                bar.getTimePeriod(),
                bar.getBeginTime(),
                contractHash
        );

        // Добавляем базовые данные свечи
        row.addFeature("open", bar.getOpenPrice());
        row.addFeature("high", bar.getHighPrice());
        row.addFeature("low", bar.getLowPrice());
        row.addFeature("close", bar.getClosePrice());
        row.addFeature("volume", bar.getVolume());

        for (ContractMetadata metadata : metadatas) {
            try {

                // Вычисляем значение фичи
                if (metadata.metadataType == MetadataType.FEATURE) {
                    Feature feature = featureRegistry.getFeature(metadata.name).orElse(null);
                    if (feature != null) {
                        FeatureTypeMetadata featureTypeMetadataByValueName = feature.getFeatureTypeMetadataByValueName(metadata.name);
                        if (featureTypeMetadataByValueName != null && featureTypeMetadataByValueName.getTimeframe().equals(timeframe)) {
                            row.addFeature(metadata.name, feature.getValueByName(metadata.name, index).bigDecimalValue());
                        } else {
                            Log.debugf("⚠️ Фича %s не поддерживает таймфрейм %s",
                                    metadata.name, timeframe);
                        }
                    } else {
                        Log.debugf("⚠️ Фича %s не существует в реестре для фич",
                                metadata.name);
                    }
                } else if (metadata.metadataType == MetadataType.LABEL) {
                    Label label = labelRegistry.getLabel(metadata.name).orElse(null);

                    if (label != null) {
                        row.addFeature(metadata.name, label.getValue(timeframe, index).intValue());
                    } else {
                        Log.debugf("⚠️ Лейбл %s не существует в реестре для лейблов",
                                metadata.name);
                    }
                }

            } catch (Exception e) {
                Log.errorf(e, "❌ Ошибка при вычислении фичи %s для свечи %s",
                        metadata.name, bar.getBeginTime());
            }
        }

        return row;
    }

}

