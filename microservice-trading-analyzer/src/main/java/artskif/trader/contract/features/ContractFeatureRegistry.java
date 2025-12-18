package artskif.trader.contract.features;

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

    private final Map<String, Feature> featureMap = new HashMap<>();

    @Inject
    public ContractFeatureRegistry(Instance<Feature> features) {
        features.forEach(creator -> {
            String featureName = creator.getFeatureName();
            featureMap.put(featureName, creator);
            Log.infof("📝 Зарегистрирована Feature: %s", featureName);
        });
    }

    /**
     * Получить фичу по имени
     */
    public Optional<Feature> getFeature(String featureName) {
        return Optional.ofNullable(featureMap.get(featureName));
    }

}

