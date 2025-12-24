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
        StringBuilder columns = new StringBuilder("tf, ts, contract_hash");
        StringBuilder values = new StringBuilder(":tf, :ts, :contractHash");

        for (String featureName : features.keySet()) {
            columns.append(", ").append(featureName);
            values.append(", :").append(featureName);
        }

        String sql = String.format("INSERT INTO features (%s) VALUES (%s)", columns, values);

        var query = entityManager.createNativeQuery(sql)
                .setParameter("tf", formatDuration(row.getTimeframe()))
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
        String checkSql = "SELECT COUNT(*) FROM features WHERE contract_hash = :contract_hash";
        Long existingCount = (Long) entityManager.createNativeQuery(checkSql)
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
                    createColumn(metadataName, feature.get().getFeatureTypeMetadataByValueName(metadataName).getDataType());
                    Log.infof("✅ Создана колонка: %s (%s)", metadataName, feature.get().getFeatureTypeMetadataByValueName(metadataName).getDataType());
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

    /**
     * Удалить контракт со всеми его метаданными и зависимыми фичами по ID
     *
     * @param contractId ID контракта для удаления
     * @return true если контракт был удален, false если контракт не найден
     */
    @Transactional
    public boolean deleteContractById(Long contractId) {
        try {
            Log.infof("🗑️ Начало удаления контракта с ID: %d", contractId);

            // Находим контракт по ID
            String findQuery = "SELECT c FROM Contract c LEFT JOIN FETCH c.metadata WHERE c.id = :contractId";
            Optional<Contract> contractOpt = entityManager.createQuery(findQuery, Contract.class)
                    .setParameter("contractId", contractId)
                    .getResultStream()
                    .findFirst();

            if (contractOpt.isEmpty()) {
                Log.warnf("⚠️ Контракт с ID '%d' не найден", contractId);
                return false;
            }

            Contract contract = contractOpt.get();
            String contractName = contract.name;
            String contractHash = contract.contractHash;

            // 1. Удаляем все строки фич из таблицы features
            String deleteFeaturesSql = "DELETE FROM features WHERE contract_hash = :contractHash";
            int deletedFeatures = entityManager.createNativeQuery(deleteFeaturesSql)
                    .setParameter("contractHash", contractHash)
                    .executeUpdate();
            Log.infof("🗑️ Удалено %d строк фич для контракта '%s'", deletedFeatures, contractName);

            // 2. Удаляем все метаданные контракта (cascade = ALL, orphanRemoval = true делает это автоматически)
            // Но для явности можем удалить вручную
            String deleteMetadataSql = "DELETE FROM contract_metadata WHERE contract_id = :contractId";
            int deletedMetadata = entityManager.createNativeQuery(deleteMetadataSql)
                    .setParameter("contractId", contractId)
                    .executeUpdate();
            Log.infof("🗑️ Удалено %d записей метаданных для контракта '%s'", deletedMetadata, contractName);

            // 3. Удаляем сам контракт
            String deleteContractSql = "DELETE FROM contracts WHERE id = :contractId";
            entityManager.createNativeQuery(deleteContractSql)
                    .setParameter("contractId", contractId)
                    .executeUpdate();

            entityManager.flush();
            Log.infof("✅ Контракт '%s' (id: %d, hash: %s) успешно удален со всеми зависимыми данными",
                    contractName, contractId, contractHash);

            return true;

        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при удалении контракта с ID: %d", contractId);
            throw new RuntimeException("Не удалось удалить контракт с ID: " + contractId, e);
        }
    }

    /**
     * Преобразует Duration в строку формата "5m", "1h", "1d"
     */
    private String formatDuration(java.time.Duration duration) {
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + "h";
        }
        long days = duration.toDays();
        return days + "d";
    }
}


