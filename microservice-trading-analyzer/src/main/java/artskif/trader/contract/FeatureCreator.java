package artskif.trader.contract;

/**
 * Интерфейс для создателей фич (features)
 * Каждый индикатор должен реализовывать этот интерфейс для добавления фич в контракт
 */
public interface FeatureCreator {

    /**
     * Получить метаданные фичи
     *
     * @return метаданные фичи
     */
    ContractFeatureMetadata getFeatureMetadata();

    /**
     * Вычислить значение фичи
     *
     * @param context контекст для вычисления (может содержать свечи, индикаторы и т.д.)
     * @return вычисленное значение фичи
     */
    Object calculateFeature(Object context);

    /**
     * Получить имя фичи
     *
     * @return имя фичи в БД
     */
    String getFeatureName();

    /**
     * Получить тип данных фичи для создания колонки в БД
     *
     * @return SQL тип данных
     */
    String getDataType();
}

