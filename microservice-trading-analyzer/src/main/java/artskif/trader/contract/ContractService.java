package artskif.trader.contract;

import artskif.trader.entity.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления контрактами и их фичами
 */
@ApplicationScoped
public class ContractService {

    @Inject
    EntityManager entityManager;

    @Inject
    ContractFeatureRegistry featureRegistry;

    /**
     * Создать или обновить контракт из свечи
     */
    @Transactional
    public Feature createOrUpdateFromCandle(Candle candle) {
        CandleId contractId = new CandleId(
                candle.id.symbol,
                candle.id.tf,
                candle.id.ts
        );

        Feature feature = entityManager.find(Feature.class, contractId);
        if (feature == null) {
            feature = new Feature(
                    contractId,
                    candle.open,
                    candle.high,
                    candle.low,
                    candle.close,
                    candle.volume,
                    candle.confirmed
            );
            entityManager.persist(feature);
        } else {
            feature.open = candle.open;
            feature.high = candle.high;
            feature.low = candle.low;
            feature.close = candle.close;
            feature.volume = candle.volume;
            feature.confirmed = candle.confirmed;
            entityManager.merge(feature);
        }

        return feature;
    }

    /**
     * Добавить фичу в контракт
     */
    @Transactional
    public void addFeatureToContract(CandleId contractId, String featureName, Object value) {
        // Проверяем, существует ли колонка для этой фичи
        ensureColumnExists(featureName);

        // Обновляем значение фичи в БД
        String sql = String.format(
                "UPDATE features SET %s = :value WHERE symbol = :symbol AND tf = :tf AND ts = :ts",
                featureName
        );

        entityManager.createNativeQuery(sql)
                .setParameter("value", value)
                .setParameter("symbol", contractId.symbol)
                .setParameter("tf", contractId.tf)
                .setParameter("ts", contractId.ts)
                .executeUpdate();

        Log.debugf("Добавлена фича %s = %s для контракта %s", featureName, value, contractId);
    }

    /**
     * Проверить и создать колонку для фичи, если её нет
     */
    public void ensureColumnExists(String featureName) {
        Optional<FeatureCreator> creatorOpt = featureRegistry.getFeatureCreator(featureName);
        if (creatorOpt.isEmpty()) {
            Log.warnf("Не найден FeatureCreator для фичи: %s", featureName);
            return;
        }

        FeatureCreator creator = creatorOpt.get();

        // Проверяем существование колонки
        if (!columnExists(featureName)) {
            createColumn(featureName, creator.getDataType());
            Log.infof("Создана новая колонка %s с типом %s", featureName, creator.getDataType());
        }

        // Регистрируем метаданные фичи, если их нет (в отдельной транзакции)
        registerFeatureMetadata(featureName, creator);
    }

    /**
     * Регистрация метаданных фичи в отдельной транзакции
     */
    @Transactional
    public void registerFeatureMetadata(String featureName, FeatureCreator creator) {
        ContractFeatureMetadata metadata = ContractFeatureMetadata.findById(featureName);
        if (metadata == null) {
            metadata = creator.getFeatureMetadata();
            metadata.persist();
            Log.infof("Зарегистрирована метаданные фичи: %s", featureName);
        }
    }

    /**
     * Проверить существование колонки в таблице features
     */
    private boolean columnExists(String columnName) {
        String sql = "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'features' AND column_name = ?";

        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter(1, columnName)
                .getSingleResult();

        return count.intValue() > 0;
    }

    /**
     * Создать новую колонку в таблице features
     */
    @Transactional
    public void createColumn(String columnName, String dataType) {
        String sql = String.format("ALTER TABLE features ADD COLUMN IF NOT EXISTS %s %s", columnName, dataType);

        try {
            entityManager.createNativeQuery(sql).executeUpdate();
            Log.infof("✅ Колонка %s добавлена в таблицу features", columnName);
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при создании колонки %s", columnName);
            throw new RuntimeException("Не удалось создать колонку " + columnName, e);
        }
    }

    /**
     * Получить все метаданные фич упорядоченные по sequence_order
     */
    public List<ContractFeatureMetadata> getAllFeatureMetadata() {
        return ContractFeatureMetadata.find("ORDER BY sequenceOrder").list();
    }
}

