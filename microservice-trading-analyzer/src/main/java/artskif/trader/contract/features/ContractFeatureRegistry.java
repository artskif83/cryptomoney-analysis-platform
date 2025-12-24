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
            List<String> valueNames = creator.getFeatureValueNames();
            valueNames.forEach(name -> featureMap.put(name, creator));
            Log.infof("📝 Зарегистрирована фича %s со значениями: %s", creator.getClass().getSimpleName(), valueNames);
        });
    }

    /**
     * Получить фичу по имени
     */
    public Optional<Feature> getFeature(String featureName) {
        return Optional.ofNullable(featureMap.get(featureName));
    }

}

