package artskif.trader.contract;

import artskif.trader.contract.features.ContractFeatureRegistry;
import artskif.trader.contract.features.Feature;
import artskif.trader.contract.labels.ContractLabelRegistry;
import artskif.trader.contract.labels.Label;
import artskif.trader.entity.Contract;
import artskif.trader.entity.MetadataType;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Сервис для работы с данными фич
 * Отвечает за сохранение FeatureRow в таблицу features
 */
@ApplicationScoped
public class ContractDataService {

    @Inject
    EntityManager entityManager;

    @Inject
    ContractFeatureRegistry featureRegistry;

    @Inject
    ContractLabelRegistry labelRegistry;
    /**
     * Вставить новую строку фич
     */
    @Transactional
    public void insertFeatureRow(FeatureRow row) {
        Map<String, Object> features = row.getAllFeatures();

        // Формируем SQL для INSERT
        StringBuilder columns = new StringBuilder("symbol, tf, ts, contract_hash");
        StringBuilder values = new StringBuilder(":symbol, :tf, :ts, :contractHash");

        for (String featureName : features.keySet()) {
            columns.append(", ").append(featureName);
            values.append(", :").append(featureName);
        }

        String sql = String.format("INSERT INTO features (%s) VALUES (%s)", columns, values);

        var query = entityManager.createNativeQuery(sql)
                .setParameter("symbol", row.getSymbol())
                .setParameter("tf", row.getTimeframe().name())
                .setParameter("ts", row.getTimestamp())
                .setParameter("contractHash", row.getContractHash());

        // Добавляем параметры для фич
        for (Map.Entry<String, Object> entry : features.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        query.executeUpdate();

        Log.tracef("💾 Вставлена новая строка фич: %s", row);
    }

    /**
     * Пакетное сохранение строк фич (оптимизированная версия)
     */
    @Transactional
    public void saveFeatureRowsBatch(Iterable<FeatureRow> rows) {
        int batchSize = 100;
        int count = 0;

        // Получаем первую строку для проверки
        var iterator = rows.iterator();
        if (!iterator.hasNext()) {
            Log.warn("⚠️ Пустой список строк для сохранения");
            return;
        }

        FeatureRow firstRow = iterator.next();

        // Проверяем, существует ли запись для этого контракта
        String checkSql = "SELECT COUNT(*) FROM features WHERE symbol = :symbol AND tf = :tf AND contract_hash = :contract_hash";
        Long existingCount = (Long) entityManager.createNativeQuery(checkSql)
                .setParameter("symbol", firstRow.getSymbol())
                .setParameter("tf", firstRow.getTimeframe().name())
                .setParameter("contract_hash", firstRow.getContractHash())
                .getSingleResult();

        if (existingCount > 0) {
            Log.error("❌ Данные для FeatureRow контракта уже существуют. Сначала удалите контракт.");
            return;
        }

        // Сохраняем первую строку
        insertFeatureRow(firstRow);
        count++;

        // Сохраняем остальные строки
        while (iterator.hasNext()) {
            FeatureRow row = iterator.next();
            insertFeatureRow(row);
            count++;

            if (count % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
                Log.debugf("💾 Сохранено %d строк фич", count);
            }
        }

        entityManager.flush();
        Log.infof("✅ Завершено пакетное сохранение: %d строк", count);
    }

    /**
     * Убедиться, что все необходимые колонки существуют
     */
    @Transactional
    public void ensureColumnExist(String metadataName, MetadataType metadataType) {
        Log.info("🔧 Проверка и создание колонок для всех фич");

        if (metadataType == MetadataType.FEATURE) {
            Optional<Feature> feature = featureRegistry.getFeature(metadataName);

            if (feature.isPresent()) {
                if (!columnExists(metadataName)) {
                    createColumn(metadataName, feature.get().getDataType());
                    Log.infof("✅ Создана колонка: %s (%s)", metadataName, feature.get().getDataType());
                }
            } else {
                Log.warnf("❌ Фича не найдена в реестре: %s", metadataName);
            }
        } else if (metadataType == MetadataType.LABEL) {
            Optional<Label> label = labelRegistry.getLabel(metadataName);

            if (label.isPresent()) {
                if (!columnExists(metadataName)) {
                    createColumn(metadataName, label.get().getDataType());
                    Log.infof("✅ Создана колонка: %s (%s)", metadataName, label.get().getDataType());
                }
            } else {
                Log.warnf("❌ Лейбл не найден в реестре: %s", metadataName);
            }
        } else {
            Log.warnf("❌ Неизвестный тип метаданных: %s для фичи: %s", metadataType, metadataName);
        }


    }

    /**
     * Проверить существование колонки
     */
    private boolean columnExists(String columnName) {
        try {
            String sql = "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'features' AND column_name = :columnName";

            var result = entityManager.createNativeQuery(sql)
                    .setParameter("columnName", columnName)
                    .getResultList();

            return !result.isEmpty();
        } catch (Exception e) {
            Log.errorf(e, "Ошибка при проверке существования колонки: %s", columnName);
            return false;
        }
    }

    /**
     * Создать колонку (вызывается из transactional метода)
     */
    private void createColumn(String columnName, String dataType) {
        try {
            String sql = String.format("ALTER TABLE features ADD COLUMN IF NOT EXISTS %s %s",
                    columnName, dataType);
            entityManager.createNativeQuery(sql).executeUpdate();
            Log.infof("✅ Создана колонка %s с типом %s", columnName, dataType);
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при создании колонки: %s", columnName);
            throw new RuntimeException("Не удалось создать колонку: " + columnName, e);
        }
    }

    /**
     * Сохранить контракт в БД
     * Если контракт с таким именем уже существует, возвращает существующий
     */
    @Transactional
    public Contract saveContract(Contract contract) {
        try {
            // Проверяем, существует ли контракт с таким именем
            // Используем JOIN FETCH для eager загрузки коллекции features
            String query = "SELECT c FROM Contract c LEFT JOIN FETCH c.metadata WHERE c.name = :name";
            Optional<Contract> existingContract = entityManager.createQuery(query, Contract.class)
                    .setParameter("name", contract.name)
                    .getResultStream()
                    .findFirst();

            if (existingContract.isPresent()) {
                Log.infof("📋 Контракт '%s' уже существует в БД (id: %d)", contract.name, existingContract.get().id);
                return existingContract.get();
            }

            // Сохраняем новый контракт
            contract.persist();
            Log.infof("✅ Контракт '%s' успешно сохранён в БД (id: %d)", contract.name, contract.id);
            return contract;

        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при сохранении контракта: %s", contract.name);
            throw new RuntimeException("Не удалось сохранить контракт", e);
        }
    }
}

