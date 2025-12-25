package artskif.trader.contract.contract;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.contract.ContractDataService;
import artskif.trader.contract.ContractRegistry;
import artskif.trader.contract.FeatureRow;
import artskif.trader.contract.features.Feature;
import artskif.trader.contract.features.FeatureTypeMetadata;
import artskif.trader.contract.labels.Label;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.entity.MetadataType;
import io.quarkus.logging.Log;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Абстрактный базовый класс для всех контрактов
 * Содержит общую логику для генерации хэша контракта
 */
public abstract class AbstractContract {

    protected final ContractDataService dataService;
    protected final ContractRegistry registry;

    protected Contract contract;
    protected String contractHash;

    public AbstractContract(ContractDataService dataService, ContractRegistry registry) {
        this.dataService = dataService;
        this.registry = registry;
    }

    public abstract String getName();

    /**
     * Инициализировать контракт с метаданными
     * Каждая реализация должна создать свой контракт с уникальными фичами и лейблами
     *
     * @return инициализированный контракт с сохраненным хешем
     */
    protected abstract Contract initializeContract();

    protected abstract Feature getBaseFeature();

    protected abstract CandleTimeframe getBaseTimeframe();

    /**
     * Сгенерировать исторические фичи и сохранить в таблицу features
     * Это используется для обучения модели ML
     */
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
        for (ContractMetadata metadata : contract.metadata) {
            dataService.ensureColumnExist(metadata.name, metadata.metadataType);
        }

        Feature baseFeature = getBaseFeature();
        if (baseFeature == null) return;

        int processedCount = 0;
        List<FeatureRow> futureRows = new ArrayList<>();
        BarSeries barSeries = baseFeature.getIndicator(getBaseTimeframe()).getBarSeries();

        for (int i = 0; i < barSeries.getBarCount(); i++) {
            FeatureRow featureRow = generateFeatureRow(getBaseTimeframe(), barSeries.getBar(i), contract.metadata, i);

            futureRows.add(featureRow);
            processedCount++;
        }

        // Сохраняем в БД
        dataService.saveFeatureRowsBatch(futureRows);

        Log.infof("✅ Завершена генерация исторических фич для контракта: %s. Обработано %d свечей",
                contract.name, processedCount);
    }

    /**
     * Генерация строки фич для одной свечи
     *
     * @param timeframe таймфрейм свечи
     * @param bar свеча
     * @param metadatas метаданные контракта (фичи и лейблы)
     * @param index индекс свечи в серии
     * @return строка фич
     */
    protected FeatureRow generateFeatureRow(CandleTimeframe timeframe, Bar bar, List<ContractMetadata> metadatas, int index) {
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
                    Feature feature = registry.getFeature(metadata.name).orElse(null);
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
                    Label label = registry.getLabel(metadata.name).orElse(null);

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

    /**
     * Сгенерировать хешкод контракта на основе всех его метаданных
     * Этот хеш будет подписывать каждую строку фич
     */
    protected String generateContractHash(Contract contract) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Формируем строку из метаданных контракта
            StringBuilder sb = new StringBuilder();
            sb.append(contract.name).append("|");
            sb.append(contract.featureSetId).append("|");

            // Добавляем все метаданные в порядке sequence_order
            List<ContractMetadata> sortedMetadata = contract.metadata.stream()
                    .sorted(Comparator.comparing(f -> f.sequenceOrder))
                    .toList();

            for (ContractMetadata metadata : sortedMetadata) {
                sb.append(metadata.name).append(":")
                  .append(metadata.dataType).append(":")
                  .append(metadata.metadataType).append(":")
                  .append(metadata.sequenceOrder).append("|");
            }

            byte[] hashBytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            // Конвертируем в hex строку
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.error("Ошибка при генерации хеша контракта", e);
            throw new RuntimeException("Не удалось создать хеш контракта", e);
        }
    }
}

