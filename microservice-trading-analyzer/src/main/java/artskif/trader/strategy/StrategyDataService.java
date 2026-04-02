package artskif.trader.strategy;

import artskif.trader.entity.ContractMetadata;
import artskif.trader.strategy.database.ColumnsRegistry;
import artskif.trader.strategy.database.columns.Column;
import artskif.trader.entity.Contract;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.snapshot.DatabaseSnapshot;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для работы с данными стратегий и фичами в базе данных.
 *
 * Основные функции:
 * - Сохранение и управление строками фич (DatabaseSnapshotRow) в таблице wide_candles
 * - Пакетная загрузка данных через промежуточную таблицу stage_wide_candles с использованием PostgreSQL COPY
 * - Управление динамическими колонками в таблицах wide_candles и stage_wide_candles
 * - CRUD операции для контрактов (Contract) и их метаданных
 * - Удаление контрактов со всеми зависимыми данными (фичи, метаданные)
 *
 * Поддерживает эффективную работу с большими объемами данных благодаря использованию
 * нативных SQL команд и оптимизированных batch операций.
 */
@ApplicationScoped
public class StrategyDataService {

    @Inject
    EntityManager entityManager;

    @Inject
    ColumnsRegistry registry;

    /**
     * Вставить или обновить строку фич (UPSERT)
     * Если строка с таким tf, tag и ts существует, она обновляется, иначе вставляется новая
     */
    @Transactional
    public void insertFeatureRow(DatabaseSnapshot row) {
        Map<String, Object> features = row.getAllColumns();

        // Формируем SQL для INSERT ... ON CONFLICT DO UPDATE
        StringBuilder columns = new StringBuilder("tf, tag, ts, contract_hash");
        StringBuilder values = new StringBuilder(":tf, :tag, :ts, :contractHash");
        StringBuilder updateSet = new StringBuilder();

        for (String featureName : features.keySet()) {
            columns.append(", ").append(featureName);
            values.append(", :").append(featureName);

            if (updateSet.length() > 0) {
                updateSet.append(", ");
            }
            updateSet.append(featureName).append(" = EXCLUDED.").append(featureName);
        }

        // Добавляем contract_hash в UPDATE SET
        if (updateSet.length() > 0) {
            updateSet.append(", ");
        }
        updateSet.append("contract_hash = EXCLUDED.contract_hash");

        String sql = String.format(
                "INSERT INTO wide_candles (%s) VALUES (%s) " +
                "ON CONFLICT (tf, tag, ts) DO UPDATE SET %s",
                columns, values, updateSet
        );

        var query = entityManager.createNativeQuery(sql)
                .setParameter("tf", formatDuration(row.getTimeframe()))
                .setParameter("tag", row.tag())
                .setParameter("ts", row.getTimestamp())
                .setParameter("contractHash", row.contractHash());

        // Добавляем параметры для фич
        for (Map.Entry<String, Object> entry : features.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        query.executeUpdate();

        Log.tracef("💾 Вставлена/обновлена строка фич: %s", row);
    }

    /**
     * Пакетное сохранение строк фич через промежуточную таблицу stage_wide_candles
     * Использует PostgreSQL COPY для быстрой загрузки данных
     */
    @Transactional
    public void saveContractSnapshotRowsBatch(Iterable<DatabaseSnapshot> rows, String tagName) {
        var iterator = rows.iterator();
        if (!iterator.hasNext()) {
            Log.warn("⚠️ Пустой список строк для сохранения");
            return;
        }

        DatabaseSnapshot firstRow = iterator.next();

        // Проверяем, существует ли запись для этого тега
        String checkSql = "SELECT COUNT(*) FROM wide_candles WHERE tag = :tagName";
        Long existingCount = (Long) entityManager.createNativeQuery(checkSql)
                .setParameter("tagName", tagName)
                .getSingleResult();

        if (existingCount > 0) {
            Log.debugf("⚠️ Найдено %d существующих записей для стратегии %s. Удаляем их...",
                    existingCount, tagName);

            String deleteSql = "DELETE FROM wide_candles WHERE tag = :tagName";
            int deleted = entityManager.createNativeQuery(deleteSql)
                    .setParameter("tagName", tagName)
                    .executeUpdate();

            Log.debugf("✅ Удалено %d записей для стратегии %s", deleted, tagName);
        }

        // Собираем все строки обратно в список для формирования CSV
        java.util.List<DatabaseSnapshot> rowList = new java.util.ArrayList<>();
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
                    stmt.execute("TRUNCATE TABLE stage_wide_candles");

                    org.postgresql.PGConnection pgConn = conn.unwrap(org.postgresql.PGConnection.class);
                    org.postgresql.copy.CopyManager cm = pgConn.getCopyAPI();

                    // Формируем список колонок для COPY
                    String columnList = buildCopyColumnList(firstRow);
                    String copySql = "COPY stage_wide_candles(" + columnList + ") " +
                            "FROM STDIN WITH (FORMAT csv, DELIMITER ',', NULL '', HEADER false)";

                    long copied = cm.copyIn(copySql, new java.io.StringReader(csv));
                    Log.debugf("💾 В staging загружено строк: %d", copied);

                    // Формируем INSERT ... SELECT с динамическими колонками
                    String upsertSql = buildUpsertSql(firstRow);
                    affected[0] = stmt.executeUpdate(upsertSql);
                    Log.debugf("💾 Upsert затронул строк: %d", affected[0]);

                    // Очищаем staging таблицу
                    stmt.execute("TRUNCATE TABLE stage_wide_candles");

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Log.debugf("✅ Завершено пакетное сохранение: %d строк", affected[0]);

        } catch (RuntimeException ex) {
            Log.errorf(ex, "❌ Ошибка при сохранении фич через COPY -> stage_wide_candles");
            throw new RuntimeException("Не удалось сохранить фичи через stage_wide_candles", ex);
        }
    }

    /**
     * Формирует CSV из списка ContractSnapshotRow
     */
    private String buildFeatureCsv(java.util.List<DatabaseSnapshot> rows) {
        return rows.stream()
                .filter(row -> row != null)
                .map(this::ContractSnapshotToCsvLine)
                .filter(line -> line != null && !line.isEmpty())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    /**
     * Преобразует ContractSnapshotRow в CSV строку
     */
    private String ContractSnapshotToCsvLine(DatabaseSnapshot row) {
        try {
            java.util.List<String> values = new java.util.ArrayList<>();

            // Добавляем базовые колонки
            values.add(formatDuration(row.getTimeframe()));
            values.add(safe(row.tag()));
            values.add(formatTimestamp(row.getTimestamp()));
            values.add(safe(row.contractHash()));

            // Добавляем фичи в отсортированном порядке
            Map<String, Object> features = row.getAllColumns();
            java.util.List<String> featureNames = new java.util.ArrayList<>(features.keySet());
            java.util.Collections.sort(featureNames);

            for (String featureName : featureNames) {
                Object value = features.get(featureName);
                values.add(formatValue(value));
            }

            return String.join(",", values);
        } catch (Exception ex) {
            Log.warnf(ex, "❌ Не удалось сформировать CSV-строку для ContractSnapshotRow: %s", row);
            return null;
        }
    }

    /**
     * Формирует список колонок для COPY команды
     */
    private String buildCopyColumnList(DatabaseSnapshot sampleRow) {
        java.util.List<String> columns = new java.util.ArrayList<>();
        columns.add("tf");
        columns.add("tag");
        columns.add("ts");
        columns.add("contract_hash");

        // Добавляем колонки фич в отсортированном порядке
        java.util.List<String> featureNames = new java.util.ArrayList<>(sampleRow.getAllColumns().keySet());
        java.util.Collections.sort(featureNames);
        columns.addAll(featureNames);

        return String.join(", ", columns);
    }

    /**
     * Формирует SQL для INSERT ... SELECT с динамическими колонками
     */
    private String buildUpsertSql(DatabaseSnapshot sampleRow) {
        java.util.List<String> featureNames = new java.util.ArrayList<>(sampleRow.getAllColumns().keySet());
        java.util.Collections.sort(featureNames);

        StringBuilder columns = new StringBuilder("tf, tag, ts, contract_hash");
        StringBuilder selectColumns = new StringBuilder("tf, tag, ts, contract_hash");
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
                "INSERT INTO wide_candles(%s) SELECT %s FROM stage_wide_candles " +
                        "ON CONFLICT (tf, tag, ts) DO UPDATE SET %s",
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
     * Проверка и создание колонок для схемы в базе данных
     *
     * @param schema схема, для которой необходимо проверить колонки
     */
    protected void checkColumnsExist(AbstractSchema schema) {
        for (ContractMetadata metadata : schema.getContract().metadata) {
            ensureColumnExist(metadata.name);
        }
    }

    /**
     * Убедиться, что все необходимые колонки существуют
     */
    @Transactional
    public void ensureColumnExist(String metadataName) {
        Log.debugf("🔧 Проверка существования колонки: %s", metadataName);

        Optional<Column> column = registry.getColumn(metadataName);

        if (column.isPresent()) {
            if (!columnExists(metadataName)) {
                createColumn(metadataName, column.get().getColumnTypeMetadataByName(metadataName).getDataType());
            }
        } else {
            Log.warnf("❌ Колонка не найдена в реестре: %s", metadataName);
        }
    }

    /**
     * Проверить существование колонки в таблицах wide_candles и stage_wide_candles
     */
    private boolean columnExists(String columnName) {
        try {
            // Проверяем существование колонки в таблице wide_candles
            String sqlFeatures = "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'wide_candles' AND column_name = :columnName";

            var resultFeatures = entityManager.createNativeQuery(sqlFeatures)
                    .setParameter("columnName", columnName)
                    .getResultList();

            // Проверяем существование колонки в таблице stage_wide_candles
            String sqlStageFeatures = "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'stage_wide_candles' AND column_name = :columnName";

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
     * Создает колонку как в основной таблице wide_candles, так и в промежуточной stage_wide_candles
     */
    private void createColumn(String columnName, String dataType) {
        try {
            // Создаем колонку в основной таблице wide_candles
            String sqlFeatures = String.format("ALTER TABLE wide_candles ADD COLUMN IF NOT EXISTS %s %s",
                    columnName, dataType);
            entityManager.createNativeQuery(sqlFeatures).executeUpdate();
            Log.infof("✅ Создана колонка %s с типом %s в таблице wide_candles", columnName, dataType);

            // Создаем колонку в промежуточной таблице stage_wide_candles
            String sqlStageFeatures = String.format("ALTER TABLE stage_wide_candles ADD COLUMN IF NOT EXISTS %s %s",
                    columnName, dataType);
            entityManager.createNativeQuery(sqlStageFeatures).executeUpdate();
            Log.infof("✅ Создана колонка %s с типом %s в таблице stage_wide_candles", columnName, dataType);
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

            // 1. Удаляем все строки фич из таблицы wide_candles
            String deleteFeaturesSql = "DELETE FROM wide_candles WHERE contract_hash = :contractHash";
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


