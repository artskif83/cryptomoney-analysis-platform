package artskif.trader.common;

import artskif.trader.indicator.IndicatorPoint;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;

public class Pipeline<T> {

    List<Stage<T>> stages;

    public Pipeline(Instance<Stage<T>> stages) {
        // Сортируем стадии по порядку выполнения
        this.stages = stages.stream()
                .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                .toList();
    }

    public T run(T context) {
        for (Stage<T> s : stages) {
            context = s.process(context); // каждый шаг обогащает данные
        }
        return context;
    }
}
