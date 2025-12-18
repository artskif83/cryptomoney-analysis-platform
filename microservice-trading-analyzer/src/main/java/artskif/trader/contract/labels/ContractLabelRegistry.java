package artskif.trader.contract.labels;

import artskif.trader.contract.labels.Label;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Реестр всех генераторов лейблов.
 */
@ApplicationScoped
public class ContractLabelRegistry {

    private final Map<String, Label> labelMap = new HashMap<>();

    @Inject
    public ContractLabelRegistry(Instance<Label> labels) {
        labels.forEach(label -> {
            String name = label.getLabelName();
            labelMap.put(name, label);
            Log.infof("📝 Зарегистрирован Label: %s", name);
        });
    }

    public Optional<Label> getLabel(String labelName) {
        return Optional.ofNullable(labelMap.get(labelName));
    }
}
