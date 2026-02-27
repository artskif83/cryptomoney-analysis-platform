package artskif.trader.strategy.database;

import artskif.trader.strategy.database.columns.Column;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Реестр .
 * Автоматически находит все бины, реализующие Label и Feature, и регистрирует их.
 */
@ApplicationScoped
public class ColumnsRegistry {

    private final Map<String, Column> columnMap = new HashMap<>();

    @Inject
    public ColumnsRegistry(Instance<Column> columns) {
        // Регистрация колонок
        columns.forEach(column -> {
            List<String> valueNames = column.getColumnNames();
            valueNames.forEach(name -> columnMap.put(name, column));
            Log.debugf("📝 Зарегистрирована колонка %s со значениями: %s", column.getClass().getSimpleName(), valueNames);
        });
    }

    /**
     * Получить колонку по имени
     */
    public Optional<Column> getColumn(String columnName) {
        return Optional.ofNullable(columnMap.get(columnName));
    }
}

