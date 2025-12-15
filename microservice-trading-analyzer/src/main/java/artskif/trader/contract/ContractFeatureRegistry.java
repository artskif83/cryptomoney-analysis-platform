package artskif.trader.contract;

import artskif.trader.entity.ContractFeatureMetadata;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.*;

/**
 * Реестр всех создателей фич
 * Автоматически находит все бины, реализующие FeatureCreator и регистрирует их
 */
@ApplicationScoped
public class ContractFeatureRegistry {

    private final Map<String, FeatureCreator> featureCreators = new HashMap<>();
    private final List<FeatureCreator> orderedCreators = new ArrayList<>();

    @Inject
    public ContractFeatureRegistry(Instance<FeatureCreator> creators) {
        creators.forEach(creator -> {
            String featureName = creator.getFeatureName();
            featureCreators.put(featureName, creator);
            orderedCreators.add(creator);
            Log.infof("📝 Зарегистрирован FeatureCreator: %s", featureName);
        });

        // Сортируем по sequence_order из метаданных
        orderedCreators.sort(Comparator.comparing(c -> {
            ContractFeatureMetadata metadata = c.getFeatureMetadata();
            return metadata.sequenceOrder;
        }));
    }

    /**
     * Получить создателя фичи по имени
     */
    public Optional<FeatureCreator> getFeatureCreator(String featureName) {
        return Optional.ofNullable(featureCreators.get(featureName));
    }

    /**
     * Получить всех создателей фич упорядоченных по sequence_order
     */
    public List<FeatureCreator> getAllCreators() {
        return Collections.unmodifiableList(orderedCreators);
    }

    /**
     * Получить все имена фич
     */
    public Set<String> getAllFeatureNames() {
        return Collections.unmodifiableSet(featureCreators.keySet());
    }
}

