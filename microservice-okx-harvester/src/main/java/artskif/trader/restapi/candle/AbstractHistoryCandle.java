package artskif.trader.restapi.candle;

import artskif.trader.common.CandleTimeframe;
import artskif.trader.kafka.KafkaProducer;
import artskif.trader.repository.CandleRepository;
import artskif.trader.restapi.config.OKXCommonConfig;
import artskif.trader.restapi.core.CandleRequest;
import artskif.trader.restapi.core.CryptoRestApiClient;
import artskif.trader.restapi.core.RetryableHttpClient;
import artskif.trader.restapi.okx.OKXHistoryRestApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Базовый класс для сбора исторических данных по свечам
 */
public abstract class AbstractHistoryCandle implements Runnable {
    private static final Logger LOG = Logger.getLogger(AbstractHistoryCandle.class);

    @Inject
    protected KafkaProducer kafkaProducer;

    @Inject
    protected CandleRepository candleRepository;

    @Inject
    protected OKXCommonConfig commonConfig;

    /**
     * Запустить харвестер асинхронно
     */
    @PostConstruct
    void onStart() {
        if (!isEnabled()) {
            LOG.infof("⚙️ Харвестер исторических свечей с таймфреймом %s отключен", getTimeframe());
            return;
        }

        LOG.infof("🚀 Запуск исторического харвестера для таймфрейма %s: instId=%s startEpochMs=%s pagesLimit=%d",
                getTimeframe(), commonConfig.getInstId(),
                Instant.ofEpochMilli(getStartEpochMs()), commonConfig.getPagesLimit());

        // Запускаем в отдельном потоке
        CompletableFuture.runAsync(this)
                .exceptionally(throwable -> {
                    LOG.errorf(throwable, "❌ Ошибка в харвестере %s", getTimeframe());
                    return null;
                });
    }

    @Override
    public void run() {
        try {
            CryptoRestApiClient<CandleRequest> apiClient = createApiClient();
            HarvestConfig config = createHarvestConfig();

            // Ищем ближайший гап в последовательности свечей
            Optional<CandleRepository.TimeGap> gapOpt = findNearestGap();

            long latestTimestamp;
            if (gapOpt.isPresent()) {
                CandleRepository.TimeGap gap = gapOpt.get();
                latestTimestamp = gap.getStartEpochMs();
                LOG.infof("📍 Найден гап: timeframe=%s начало=%s (%d) конец=%s (%d)",
                        getTimeframe(),
                        gap.getStart(), latestTimestamp,
                        gap.getEnd(), gap.getEndEpochMs());
            } else {
                // Если гапов нет, используем последнюю свечу
                latestTimestamp = getLatestTimestamp();
                LOG.infof("📍 Гапы не найдены. Граница: timeframe=%s stopAt=%d (%s)",
                        getTimeframe(), latestTimestamp, Instant.ofEpochMilli(latestTimestamp));
            }

            harvest(apiClient, latestTimestamp, config);

            LOG.infof("✅ Исторический харвестер %s завершил работу", getTimeframe());
        } catch (Exception e) {
            LOG.errorf(e, "❌ Критическая ошибка в историческом харвестере %s", getTimeframe());
        }
    }

    /**
     * Основная логика сбора данных
     */
    protected void harvest(CryptoRestApiClient<CandleRequest> apiClient, long latestTimestamp, HarvestConfig config) {
        String timeframe = getTimeframe();
        String topic = buildTopicName(timeframe);
        LOG.infof("📥 Harvest: timeframe=%s -> topic=%s", timeframe, topic);

        Long to = null;
        Long from = latestTimestamp;
        int pagesLoaded = 0;

        while (pagesLoaded < config.pagesLimit()) {
            CandleRequest request = CandleRequest.builder()
                    .instId(config.instId())
                    .timeframe(timeframe)
                    .limit(config.limit())
                    .before(from)
                    .after(to)
                    .build();

            Optional<JsonNode> rootOpt = apiClient.fetchCandles(request);
            if (rootOpt.isEmpty()) {
                LOG.warnf("⚠️ Пропуск страницы для timeframe=%s", timeframe);
                break;
            }

            JsonNode data = rootOpt.get().path("data");
            if (!data.isArray() || data.isEmpty()) {
                LOG.infof("🏁 Данных больше нет: timeframe=%s", timeframe);
                break;
            }

            long minTs = extractMinTimestamp(data);
            logCandleData(timeframe, data);

            boolean isLast = (to == null);
            String payload = buildPayload(config.instId(), isLast, data);
            kafkaProducer.sendMessage(topic, payload);

            if (minTs <= latestTimestamp) {
                LOG.infof("⛳ Граница достигнута: minTs=%d <= %d для timeframe=%s",
                        minTs, latestTimestamp, timeframe);
                break;
            }

            pagesLoaded++;
            LOG.infof("📦 Страница #%d (%d записей) для timeframe=%s; minTs=%d (%s)",
                    pagesLoaded, data.size(), timeframe, minTs, Instant.ofEpochMilli(minTs));

            to = minTs - 1;
            sleep(config.requestPauseMs());
        }
    }

    private CryptoRestApiClient<CandleRequest> createApiClient() {
        RetryableHttpClient httpClient = new RetryableHttpClient(
                commonConfig.getMaxRetries(),
                commonConfig.getRetryBackoffMs()
        );
        return new OKXHistoryRestApiClient(commonConfig.getBaseUrl(), httpClient);
    }

    private HarvestConfig createHarvestConfig() {
        return HarvestConfig.builder()
                .instId(commonConfig.getInstId())
                .limit(commonConfig.getLimit())
                .startEpochMs(getStartEpochMs())
                .requestPauseMs(commonConfig.getRequestPauseMs())
                .pagesLimit(commonConfig.getPagesLimit())
                .build();
    }

    private long getLatestTimestamp() {
        return candleRepository.getLatestCandleTimestamp(
                commonConfig.getInstId(),
                getDbTimeframeKey(),
                getStartEpochMs()
        );
    }

    /**
     * Находит ближайший к текущему времени временной разрыв (гап) в последовательности свечей.
     * Если гап не найден, возвращает Optional.empty()
     */
    private Optional<CandleRepository.TimeGap> findNearestGap() {
        return candleRepository.findNearestGap(
                commonConfig.getInstId(),
                getDbTimeframeKey(),
                getTimeframeType().getDuration(),
                getStartEpochMs()
        );
    }

    private String buildTopicName(String timeframe) {
        return "okx-candle-" + normalizeTimeframe(timeframe) + "-history";
    }

    private long extractMinTimestamp(JsonNode data) {
        long minTs = Long.MAX_VALUE;
        for (JsonNode arr : data) {
            long ts = arr.get(0).asLong();
            if (ts < minTs) minTs = ts;
        }
        return minTs;
    }

    private void logCandleData(String timeframe, JsonNode data) {
        if (!LOG.isDebugEnabled()) return;

        LOG.debugf("📊 Данные для timeframe=%s:", timeframe);
        for (JsonNode arr : data) {
            if (arr.isArray() && arr.size() >= 6) {
                LOG.debugf("  🕐 %s | O:%.2f H:%.2f L:%.2f C:%.2f V:%.2f",
                        Instant.ofEpochMilli(arr.get(0).asLong()),
                        arr.get(1).asDouble(), arr.get(2).asDouble(),
                        arr.get(3).asDouble(), arr.get(4).asDouble(),
                        arr.get(5).asDouble());
            }
        }
    }

    private String buildPayload(String instId, boolean isLast, JsonNode data) {
        return String.format("{\"instId\":\"%s\",\"isLast\":%s,\"data\":%s}",
                instId, isLast, data);
    }

    private String normalizeTimeframe(String timeframe) {
        return timeframe.toLowerCase()
                .replace("h", "h")
                .replace("w", "w")
                .replace("m", "m");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Получить таймфрейм для API запроса (например "1m", "4H", "1W")
     */
    protected abstract String getTimeframe();

    /**
     * Получить тип таймфрейма
     */
    protected abstract CandleTimeframe getTimeframeType();

    /**
     * Получить ключ таймфрейма для БД (например "CANDLE_1M", "CANDLE_4H")
     */
    protected abstract String getDbTimeframeKey();

    /**
     * Проверить, включен ли харвестер
     */
    protected abstract boolean isEnabled();

    /**
     * Получить начальную дату для загрузки (epoch ms)
     */
    protected abstract long getStartEpochMs();
}

