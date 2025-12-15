package artskif.trader.contract;

import artskif.trader.contract.features.RsiFeatureContext;
import artskif.trader.entity.Candle;
import artskif.trader.entity.CandleId;
import artskif.trader.entity.Feature;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Процессор контрактов - обрабатывает свечи и создает контракты с фичами
 */
@ApplicationScoped
public class ContractProcessor {

    @Inject
    ContractService contractService;

    @Inject
    ContractFeatureRegistry featureRegistry;

    /**
     * Обработать свечу и создать/обновить контракт
     */
    @Transactional
    public void processCandle(Candle candle) {
        try {
            // 1. Создаем или обновляем базовый контракт из свечи
            Feature contract = contractService.createOrUpdateFromCandle(candle);

            // 2. Получаем исторические данные для расчета индикаторов
            List<Candle> historicalCandles = getHistoricalCandles(
                    candle.id.symbol,
                    candle.id.tf,
                    candle.id.ts,
                    20 // Берем 20 свечей для расчета индикаторов
            );

            // 3. Вычисляем и добавляем все фичи
            featureRegistry.getAllCreators().forEach(creator -> {
                try {
                    processFeature(contract.id, creator, historicalCandles, candle);
                } catch (Exception e) {
                    Log.errorf(e, "Ошибка при обработке фичи %s для контракта %s",
                            creator.getFeatureName(), contract.id);
                }
            });

            Log.debugf("✅ Обработан контракт для свечи %s", candle.id);

        } catch (Exception e) {
            Log.errorf(e, "Ошибка при обработке свечи %s", candle.id);
        }
    }

    /**
     * Обработать отдельную фичу
     */
    @Transactional
    public void processFeature(CandleId contractId, FeatureCreator creator,
                               List<Candle> historicalCandles, Candle currentCandle) {

        String featureName = creator.getFeatureName();

        // Убеждаемся, что колонка существует
        contractService.ensureColumnExists(featureName);

        // Создаем контекст для вычисления
        Object context = createContext(creator, historicalCandles, currentCandle);

        // Вычисляем значение фичи
        Object featureValue = creator.calculateFeature(context);

        if (featureValue != null) {
            // Сохраняем значение фичи в БД
            contractService.addFeatureToContract(contractId, featureName, featureValue);
            Log.debugf("📊 Добавлена фича %s = %s для %s", featureName, featureValue, contractId);
        } else {
            Log.debugf("⚠️ Не удалось вычислить фичу %s для %s", featureName, contractId);
        }
    }

    /**
     * Создать контекст для расчета фичи
     */
    private Object createContext(FeatureCreator creator, List<Candle> historicalCandles, Candle currentCandle) {
        // В зависимости от типа фичи создаем разный контекст
        String featureName = creator.getFeatureName();

        if (featureName.startsWith("rsi")) {
            return new RsiFeatureContext(historicalCandles, currentCandle);
        }

        // Для других типов индикаторов можно добавить другие контексты
        return null;
    }

    /**
     * Получить исторические свечи для расчета индикаторов
     */
    private List<Candle> getHistoricalCandles(String symbol, String tf, Instant currentTs, int count) {
        // Вычисляем временной диапазон в зависимости от таймфрейма
        long minutesBack = getTimeframeMinutes(tf) * count;
        Instant startTs = currentTs.minus(minutesBack, ChronoUnit.MINUTES);

        return Candle.find(
                "id.symbol = ?1 AND id.tf = ?2 AND id.ts >= ?3 AND id.ts <= ?4 ORDER BY id.ts ASC",
                symbol, tf, startTs, currentTs
        ).list();
    }

    /**
     * Получить количество минут для таймфрейма
     */
    private long getTimeframeMinutes(String tf) {
        return switch (tf) {
            case "1m" -> 1;
            case "5m" -> 5;
            case "15m" -> 15;
            case "1h" -> 60;
            case "4h" -> 240;
            case "1d" -> 1440;
            case "1w" -> 10080;
            default -> 5; // По умолчанию 5 минут
        };
    }

    /**
     * Обработать все подтвержденные свечи для создания контрактов
     */
    @Transactional
    public void processConfirmedCandles(String symbol, String tf, Instant from, Instant to) {
        List<Candle> candles = Candle.find(
                "id.symbol = ?1 AND id.tf = ?2 AND id.ts >= ?3 AND id.ts <= ?4 AND confirmed = true ORDER BY id.ts ASC",
                symbol, tf, from, to
        ).list();

        Log.infof("🔄 Начинаем обработку %d подтвержденных свечей для %s %s", candles.size(), symbol, tf);

        int processed = 0;
        for (Candle candle : candles) {
            processCandle(candle);
            processed++;

            if (processed % 100 == 0) {
                Log.infof("Обработано %d/%d свечей", processed, candles.size());
            }
        }

        Log.infof("✅ Обработка завершена: %d свечей", processed);
    }
}

