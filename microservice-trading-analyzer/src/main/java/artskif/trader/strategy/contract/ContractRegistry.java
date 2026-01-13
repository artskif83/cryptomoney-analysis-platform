package artskif.trader.strategy.contract;

import artskif.trader.strategy.contract.features.Feature;
import artskif.trader.strategy.contract.labels.Label;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Реестр всех генераторов лейблов и создателей фич.
 * Автоматически находит все бины, реализующие Label и Feature, и регистрирует их.
 */
@ApplicationScoped
public class ContractRegistry {

    private final Map<String, Label> labelMap = new HashMap<>();
    private final Map<String, Feature> featureMap = new HashMap<>();

    @Inject
    public ContractRegistry(Instance<Label> labels, Instance<Feature> features) {
        // Регистрация лейблов
        labels.forEach(label -> {
            String name = label.getLabelName();
            labelMap.put(name, label);
            Log.infof("📝 Зарегистрирован Label: %s", name);
        });

        // Регистрация фич
        features.forEach(feature -> {
            List<String> valueNames = feature.getFeatureValueNames();
            valueNames.forEach(name -> featureMap.put(name, feature));
            Log.infof("📝 Зарегистрирована фича %s со значениями: %s", feature.getClass().getSimpleName(), valueNames);
        });
    }

    /**
     * Получить лейбл по имени
     */
    public Optional<Label> getLabel(String labelName) {
        return Optional.ofNullable(labelMap.get(labelName));
    }

    /**
     * Получить фичу по имени
     */
    public Optional<Feature> getFeature(String featureName) {
        return Optional.ofNullable(featureMap.get(featureName));
    }
}

