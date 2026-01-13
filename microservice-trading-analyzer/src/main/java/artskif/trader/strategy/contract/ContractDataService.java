package artskif.trader.strategy.contract;

import artskif.trader.strategy.contract.features.Feature;
import artskif.trader.strategy.contract.labels.Label;
import artskif.trader.entity.Contract;
import artskif.trader.entity.MetadataType;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
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
    ContractRegistry registry;

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
     * Пакетное сохранение строк фич через промежуточную таблицу stage_features
     * Использует PostgreSQL COPY для быстрой загрузки данных
     */
    @Transactional
    public void saveFeatureRowsBatch(Iterable<FeatureRow> rows) {
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

        // Собираем все строки обратно в список для формирования CSV
        java.util.List<FeatureRow> rowList = new java.util.ArrayList<>();
        rowList.add(firstRow);
        iterator.forEachRemaining(rowList::add);

        // Строим CSV из всех строк фич
        String csv = buildFeatureCsv(rowList);

        if (csv.isEmpty()) {
            Log.warn("⚠️ Не удалось сформировать CSV для вставки");
            return;
        }

        final int[] affected = new int[1];
        org.hibernate.Session session = entityManager.unwrap(org.hibernate.Session.class);

        try {
            session.doWork(conn -> {
                try (java.sql.Statement stmt = conn.createStatement()) {
                    // Очищаем staging таблицу
                    stmt.execute("TRUNCATE TABLE stage_features");

                    org.postgresql.PGConnection pgConn = conn.unwrap(org.postgresql.PGConnection.class);
                    org.postgresql.copy.CopyManager cm = pgConn.getCopyAPI();

                    // Формируем список колонок для COPY
                    String columnList = buildCopyColumnList(firstRow);
                    String copySql = "COPY stage_features(" + columnList + ") " +
                            "FROM STDIN WITH (FORMAT csv, DELIMITER ',', NULL '', HEADER false)";

                    long copied = cm.copyIn(copySql, new java.io.StringReader(csv));
                    Log.debugf("💾 В staging загружено строк: %d", copied);

                    // Формируем INSERT ... SELECT с динамическими колонками
                    String upsertSql = buildUpsertSql(firstRow);
                    affected[0] = stmt.executeUpdate(upsertSql);
                    Log.debugf("✅ Upsert затронул строк: %d", affected[0]);

                    // Очищаем staging таблицу
                    stmt.execute("TRUNCATE TABLE stage_features");

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Log.infof("✅ Завершено пакетное сохранение: %d строк", affected[0]);

        } catch (RuntimeException ex) {
            Log.errorf(ex, "❌ Ошибка при сохранении фич через COPY -> stage_features");
            throw new RuntimeException("Не удалось сохранить фичи через stage_features", ex);
        }
    }

    /**
     * Формирует CSV из списка FeatureRow
     */
    private String buildFeatureCsv(java.util.List<FeatureRow> rows) {
        return rows.stream()
                .filter(row -> row != null)
                .map(this::featureRowToCsvLine)
                .filter(line -> line != null && !line.isEmpty())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    /**
     * Преобразует FeatureRow в CSV строку
     */
    private String featureRowToCsvLine(FeatureRow row) {
        try {
            java.util.List<String> values = new java.util.ArrayList<>();

            // Добавляем базовые колонки
            values.add(formatDuration(row.getTimeframe()));
            values.add(formatTimestamp(row.getTimestamp()));
            values.add(safe(row.getContractHash()));

            // Добавляем фичи в отсортированном порядке
            Map<String, Object> features = row.getAllFeatures();
            java.util.List<String> featureNames = new java.util.ArrayList<>(features.keySet());
            java.util.Collections.sort(featureNames);

            for (String featureName : featureNames) {
                Object value = features.get(featureName);
                values.add(formatValue(value));
            }

            return String.join(",", values);
        } catch (Exception ex) {
            Log.warnf(ex, "❌ Не удалось сформировать CSV-строку для FeatureRow: %s", row);
            return null;
        }
    }

    /**
     * Формирует список колонок для COPY команды
     */
    private String buildCopyColumnList(FeatureRow sampleRow) {
        java.util.List<String> columns = new java.util.ArrayList<>();
        columns.add("tf");
        columns.add("ts");
        columns.add("contract_hash");

        // Добавляем колонки фич в отсортированном порядке
        java.util.List<String> featureNames = new java.util.ArrayList<>(sampleRow.getAllFeatures().keySet());
        java.util.Collections.sort(featureNames);
        columns.addAll(featureNames);

        return String.join(", ", columns);
    }

    /**
     * Формирует SQL для INSERT ... SELECT с динамическими колонками
     */
    private String buildUpsertSql(FeatureRow sampleRow) {
        java.util.List<String> featureNames = new java.util.ArrayList<>(sampleRow.getAllFeatures().keySet());
        java.util.Collections.sort(featureNames);

        StringBuilder columns = new StringBuilder("tf, ts, contract_hash");
        StringBuilder selectColumns = new StringBuilder("tf, ts, contract_hash");
        StringBuilder updateSet = new StringBuilder();

        for (String featureName : featureNames) {
            columns.append(", ").append(featureName);
            selectColumns.append(", ").append(featureName);
            if (updateSet.length() > 0) {
                updateSet.append(", ");
            }
            updateSet.append(featureName).append(" = EXCLUDED.").append(featureName);
        }

        return String.format(
                "INSERT INTO features(%s) SELECT %s FROM stage_features " +
                        "ON CONFLICT (tf, ts) DO UPDATE SET %s",
                columns, selectColumns, updateSet
        );
    }

    /**
     * Форматирует значение для CSV
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) value).toPlainString();
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        return String.valueOf(value);
    }

    /**
     * Форматирует timestamp для CSV
     */
    private String formatTimestamp(Instant timestamp) {
        if (timestamp == null) {
            return "";
        }
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return formatter.format(java.time.LocalDateTime.ofInstant(timestamp, java.time.ZoneOffset.UTC));
    }

    /**
     * Безопасное преобразование строки
     */
    private String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Убедиться, что все необходимые колонки существуют
     */
    @Transactional
    public void ensureColumnExist(String metadataName, MetadataType metadataType) {
        Log.infof("🔧 Проверка существования колонки: %s", metadataName);

        if (metadataType == MetadataType.FEATURE) {
            Optional<Feature> feature = registry.getFeature(metadataName);

            if (feature.isPresent()) {
                if (!columnExists(metadataName)) {
                    createColumn(metadataName, feature.get().getFeatureTypeMetadataByValueName(metadataName).getDataType());
                }
            } else {
                Log.warnf("❌ Фича не найдена в реестре: %s", metadataName);
            }
        } else if (metadataType == MetadataType.LABEL) {
            Optional<Label> label = registry.getLabel(metadataName);

            if (label.isPresent()) {
                if (!columnExists(metadataName)) {
                    createColumn(metadataName, label.get().getDataType());
                }
            } else {
                Log.warnf("❌ Лейбл не найден в реестре: %s", metadataName);
            }
        } else {
            Log.warnf("❌ Неизвестный тип метаданных: %s для фичи: %s", metadataType, metadataName);
        }


    }

    /**
     * Проверить существование колонки в таблицах features и stage_features
     */
    private boolean columnExists(String columnName) {
        try {
            // Проверяем существование колонки в таблице features
            String sqlFeatures = "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'features' AND column_name = :columnName";

            var resultFeatures = entityManager.createNativeQuery(sqlFeatures)
                    .setParameter("columnName", columnName)
                    .getResultList();

            // Проверяем существование колонки в таблице stage_features
            String sqlStageFeatures = "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'stage_features' AND column_name = :columnName";

            var resultStageFeatures = entityManager.createNativeQuery(sqlStageFeatures)
                    .setParameter("columnName", columnName)
                    .getResultList();

            // Колонка должна существовать в обеих таблицах
            return !resultFeatures.isEmpty() && !resultStageFeatures.isEmpty();
        } catch (Exception e) {
            Log.errorf(e, "Ошибка при проверке существования колонки: %s", columnName);
            return false;
        }
    }

    /**
     * Создать колонку (вызывается из transactional метода)
     * Создает колонку как в основной таблице features, так и в промежуточной stage_features
     */
    private void createColumn(String columnName, String dataType) {
        try {
            // Создаем колонку в основной таблице features
            String sqlFeatures = String.format("ALTER TABLE features ADD COLUMN IF NOT EXISTS %s %s",
                    columnName, dataType);
            entityManager.createNativeQuery(sqlFeatures).executeUpdate();
            Log.infof("✅ Создана колонка %s с типом %s в таблице features", columnName, dataType);

            // Создаем колонку в промежуточной таблице stage_features
            String sqlStageFeatures = String.format("ALTER TABLE stage_features ADD COLUMN IF NOT EXISTS %s %s",
                    columnName, dataType);
            entityManager.createNativeQuery(sqlStageFeatures).executeUpdate();
            Log.infof("✅ Создана колонка %s с типом %s в таблице stage_features", columnName, dataType);
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при создании колонки: %s", columnName);
            throw new RuntimeException("Не удалось создать колонку: " + columnName, e);
        }
    }

    /**
     * Найти контракт по имени
     *
     * @param name имя контракта
     * @return контракт или null если не найден
     */
    @Transactional
    public Contract findContractByName(String name) {
        try {
            // Используем JOIN FETCH для eager загрузки коллекции metadata
            String query = "SELECT c FROM Contract c LEFT JOIN FETCH c.metadata WHERE c.name = :name";
            return entityManager.createQuery(query, Contract.class)
                    .setParameter("name", name)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при поиске контракта: %s", name);
            return null;
        }
    }

    /**
     * Сохранить новый контракт в БД (без проверки существования)
     *
     * @param contract контракт для сохранения
     * @return сохранённый контракт
     */
    @Transactional
    public Contract saveNewContract(Contract contract) {
        try {
            contract.persist();
            entityManager.flush(); // Сразу сбрасываем в БД для получения ID
            Log.infof("✅ Контракт '%s' успешно создан и сохранён в БД (id: %d)", contract.name, contract.id);
            return contract;
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при сохранении нового контракта: %s", contract.name);
            throw new RuntimeException("Не удалось сохранить новый контракт", e);
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


