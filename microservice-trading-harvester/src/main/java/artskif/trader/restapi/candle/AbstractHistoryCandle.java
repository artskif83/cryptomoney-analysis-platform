package artskif.trader.restapi.candle;

import artskif.trader.common.CandleTimeframe;
import artskif.trader.kafka.KafkaProducer;
import artskif.trader.repository.CandleRepository;
import artskif.trader.repository.TimeGap;
import artskif.trader.restapi.config.OKXCommonConfig;
import artskif.trader.restapi.core.CandleRequest;
import artskif.trader.restapi.core.CryptoRestApiClient;
import artskif.trader.restapi.core.RetryableHttpClient;
import artskif.trader.restapi.okx.OKXHistoryRestApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Базовый класс для сбора исторических данных по свечам
 */
public abstract class AbstractHistoryCandle {
    private static final Logger LOG = Logger.getLogger(AbstractHistoryCandle.class);

    @Inject
    protected KafkaProducer kafkaProducer;

    @Inject
    protected CandleRepository candleRepository;

    @Inject
    protected OKXCommonConfig commonConfig;

    /**
     * Флаг для предотвращения одновременного выполнения нескольких синхронизаций
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Флаг остановки приложения — при shutdown все циклы должны прерваться
     */
    private volatile boolean shuttingDown = false;

    /**
     * Отдельный поток для первоначальной синхронизации при старте.
     * Хранится, чтобы можно было прервать при получении ShutdownEvent.
     */
    private final ExecutorService startupExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setName("harvester-startup-" + getClass().getSimpleName());
        t.setDaemon(true);
        return t;
    });
    private volatile Future<?> startupFuture;

    void onStop(@Observes ShutdownEvent ev) {
        shuttingDown = true;
        LOG.infof("🛑 Получен сигнал остановки харвестера %s, прерываем текущую синхронизацию...", getTimeframe());

        // Прерываем поток, если он ещё выполняется
        Future<?> future = startupFuture;
        if (future != null && !future.isDone()) {
            future.cancel(true); // true = interrupt the thread
        }

        startupExecutor.shutdownNow();
        try {
            if (!startupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOG.warnf("⚠️ Поток харвестера %s не завершился за 5 секунд", getTimeframe());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void onStart(@Observes StartupEvent ev) {
        if (!isEnabled()) {
            LOG.infof("⚙️ Харвестер исторических свечей с таймфреймом %s отключен", getTimeframe());
            return;
        }
        // Запускаем в отдельном потоке, чтобы не блокировать поток StartupEvent
        // и чтобы поток можно было корректно прервать при ShutdownEvent
        startupFuture = startupExecutor.submit(this::syncScheduled);
    }

    /**
     * Запуск синхронизации по расписанию.
     * Метод должен вызываться из @Scheduled методов в конкретных классах-наследниках.
     * Реализует защиту от повторного запуска через AtomicBoolean флаг.
     * Выполняется асинхронно в отдельном потоке.
     */
    protected void syncScheduled() {
        if (!isEnabled()) {
            return;
        }

        if (shuttingDown) {
            LOG.infof("🛑 Приложение останавливается, пропускаем запуск синхронизации %s", getTimeframe());
            return;
        }

        // Проверяем, не выполняется ли уже синхронизация
        if (!isRunning.compareAndSet(false, true)) {
            LOG.warnf("⏳ Синхронизация %s уже выполняется, пропускаем текущий запуск", getTimeframe());
            return;
        }

        try {
            LOG.info("🚀 // -------------------------------------------------------------------------------");

            LOG.infof("🚀 Запуск синхронизации для таймфрейма %s: instId=%s startEpochMs=%s pagesLimit=%d",
                    getTimeframe(), commonConfig.getInstId(),
                    Instant.ofEpochMilli(getStartEpochMs()), commonConfig.getPagesLimit());

            runSync();

            LOG.infof("✅ Синхронизация %s завершена", getTimeframe());
        } catch (Exception e) {
            LOG.errorf(e, "❌ Ошибка в синхронизации %s", getTimeframe());
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * Основной метод синхронизации данных
     */
    protected void runSync() {
        CryptoRestApiClient<CandleRequest> apiClient = createApiClient();
        HarvestConfig config = createHarvestConfig();

        // Ищем все гапы в последовательности свечей
        List<TimeGap> allGaps = findAllGaps();

        if (allGaps.isEmpty()) {
            LOG.infof("✅ Гапы не найдены для %s, данные полные", getTimeframe());
            return;
        }

        LOG.infof("📋 Найдено %d гапов для заполнения, таймфрейм: %s", allGaps.size(), getTimeframe());
        harvest(apiClient, allGaps, config);
    }

    /**
     * Основная логика сбора данных для всех гапов
     */
    protected void harvest(CryptoRestApiClient<CandleRequest> apiClient, List<TimeGap> timeGaps, HarvestConfig config) {
        String timeframe = getTimeframe();
        String topic = buildTopicName(timeframe);

        LOG.infof("📥 Harvest: timeframe=%s -> topic=%s, гапов для обработки: %d", timeframe, topic, timeGaps.size());

        int totalPagesLoaded = 0;
        int gapNumber = 0;

        // Обрабатываем каждый гап
        for (TimeGap gap : timeGaps) {
            if (shuttingDown || Thread.currentThread().isInterrupted()) {
                LOG.infof("🛑 Остановка во время обхода гапов для %s", timeframe);
                break;
            }

            gapNumber++;
            Long gapStartMs = gap.getStartEpochMs();
            Long gapEndMs = gap.getEndEpochMs();

            LOG.infof("🔧 Обработка гапа #%d/%d: %s", gapNumber, timeGaps.size(), gap);

            // Для каждого гапа запрашиваем данные с постраничным разбиением
            boolean isLastGap = (gapNumber == timeGaps.size());
            int gapPagesLoaded = harvestGap(apiClient, config, timeframe, topic, gapStartMs, gapEndMs, gapNumber, timeGaps.size(), isLastGap);
            totalPagesLoaded += gapPagesLoaded;

            LOG.infof("✅ Гап #%d обработан, загружено страниц: %d", gapNumber, gapPagesLoaded);

            // Проверяем общий лимит страниц
            if (config.pagesLimit() > 0 && totalPagesLoaded >= config.pagesLimit()) {
                LOG.warnf("⚠️ Достигнут общий лимит страниц: %d, остановка харвестера", config.pagesLimit());
                break;
            }
        }

        LOG.infof("📊 Итого загружено страниц для всех гапов: %d", totalPagesLoaded);
    }

    /**
     * Обрабатывает один гап с постраничным разбиением
     *
     * @param apiClient  клиент для запросов
     * @param config     конфигурация харвестера
     * @param timeframe  таймфрейм свечей
     * @param topic      топик Kafka для отправки
     * @param gapStartMs начало гапа в миллисекундах
     * @param gapEndMs   конец гапа в миллисекундах
     * @param gapNumber  номер текущего гапа
     * @param totalGaps  общее количество гапов
     * @param isLastGap  является ли этот гап последним в списке
     * @return количество загруженных страниц
     */
    private int harvestGap(CryptoRestApiClient<CandleRequest> apiClient, HarvestConfig config,
                           String timeframe, String topic, Long gapStartMs, Long gapEndMs,
                           int gapNumber, int totalGaps, boolean isLastGap) {

        // OKX API: before - верхняя граница (более поздние свечи), after - нижняя граница (более ранние свечи)
        // Запрашиваем от конца гапа (gapEndMs) к началу (gapStartMs)
        Long before = gapEndMs;  // Начинаем с конца гапа
        Long after = gapStartMs;  // Не выходим за начало гапа

        int pagesLoaded = 0;

        while (!shuttingDown && !Thread.currentThread().isInterrupted() &&
                (config.pagesLimit() == 0 || pagesLoaded < config.pagesLimit())) {
            CandleRequest request = CandleRequest.builder()
                    .instId(config.instId())
                    .timeframe(timeframe)
                    .limit(config.limit())
                    .before(before)
                    .after(after)
                    .build();

            Optional<JsonNode> rootOpt = apiClient.fetchCandles(request);
            if (rootOpt.isEmpty()) {
                LOG.warnf("⚠️ Пропуск страницы для timeframe=%s в гапе [%d - %d]",
                        timeframe, gapStartMs, gapEndMs);
                break;
            }

            JsonNode data = rootOpt.get().path("data");
            if (!data.isArray() || data.isEmpty()) {
                LOG.infof("🏁 Данных больше нет в гапе [%d - %d] для timeframe=%s",
                        gapStartMs, gapEndMs, timeframe);
                break;
            }

            long minTs = extractMinTimestamp(data);

            // Проверяем, является ли это последней страницей последнего гапа
            // isLast = true только если:
            // 1. Это последний гап (isLastGap == true)
            // 2. И мы достигли начала гапа (minTs <= gapStartMs) или следующая итерация выйдет за границу
            boolean isReachedGapStart = minTs <= gapStartMs + getTimeframeType().getDuration().toMillis();
            boolean willExceedGapStart = (minTs - 1) < gapStartMs + getTimeframeType().getDuration().toMillis();
            boolean isLast = isLastGap && (isReachedGapStart || willExceedGapStart);

            // Детальное логирование с информацией о гапе и конфигурации
            logCandleData(timeframe, data, gapNumber, totalGaps, gapStartMs, gapEndMs, minTs, isLast, config);

            String payload = buildPayload(config.instId(), isLast, data);
            kafkaProducer.sendMessage(topic, payload);

            pagesLoaded++;
            LOG.infof("📦 Страница #%d (%d записей) для timeframe=%s в гапе; minTs=%d (%s); isLast=%s",
                    pagesLoaded, data.size(), timeframe, minTs, Instant.ofEpochMilli(minTs), isLast);

            // Проверяем, достигли ли мы начала гапа
            if (isReachedGapStart) {
                LOG.infof("⛳ Граница гапа достигнута: minTs=%d <= gapStart=%d для timeframe=%s",
                        minTs, gapStartMs, timeframe);
                break;
            }

            // Двигаемся дальше в прошлое
            before = minTs - 1;

            // Убеждаемся, что не вышли за границу гапа
            if (willExceedGapStart) {
                LOG.infof("⛳ before=%d вышел за начало гапа=%d, останавливаемся", before, gapStartMs);
                break;
            }

            sleep(config.requestPauseMs());
        }

        return pagesLoaded;
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

    /**
     * Находит ближайший к текущему времени временной разрыв (гап) в последовательности свечей.
     * Если гап не найден, возвращает Optional.empty()
     */
    private List<TimeGap> findAllGaps() {
        return candleRepository.findAllGaps(
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

    private void logCandleData(String timeframe, JsonNode data, int gapNumber, int totalGaps,
                               Long gapStartMs, Long gapEndMs, long minTs, boolean isLast, HarvestConfig config) {
        if (!LOG.isDebugEnabled() || !data.isArray() || data.isEmpty()) return;

        // Получаем первую и последнюю свечу
        JsonNode firstCandle = data.get(0);
        JsonNode lastCandle = data.get(data.size() - 1);

        if (firstCandle.isArray() && !firstCandle.isEmpty() &&
                lastCandle.isArray() && !lastCandle.isEmpty()) {

            Instant firstTs = Instant.ofEpochMilli(firstCandle.get(0).asLong());
            Instant lastTs = Instant.ofEpochMilli(lastCandle.get(0).asLong());

            // Обработка null значений для gapStartMs и gapEndMs
            String gapStartStr = gapStartMs != null ? Instant.ofEpochMilli(gapStartMs).toString() : "null";
            String gapEndStr = gapEndMs != null ? Instant.ofEpochMilli(gapEndMs).toString() : "null";
            String gapStartMsStr = gapStartMs != null ? gapStartMs.toString() : "null";
            String gapEndMsStr = gapEndMs != null ? gapEndMs.toString() : "null";
            Instant minTsTime = Instant.ofEpochMilli(minTs);

            LOG.debugf("""
                            📊 ══════════════════════════════════════════════════════════════════════════════════
                            📊 HARVEST DATA | Timeframe: %s | Gap: #%d/%d | isLast: %s
                            📊 ──────────────────────────────────────────────────────────────────────────────────
                            📊 Гап:      %s (%s) ➜ %s (%s)
                            📊 Свечи:    %s ➜ %s (всего: %d)
                            📊 Мин. время выборки:    %s (%d)
                            📊 ──────────────────────────────────────────────────────────────────────────────────
                            📊 Config:   instId=%s | limit=%d | startEpochMs=%s (%d) | pause=%dms | pages=%d
                            📊 ══════════════════════════════════════════════════════════════════════════════════""",
                    timeframe, gapNumber, totalGaps, isLast,
                    gapStartStr, gapStartMsStr, gapEndStr, gapEndMsStr,
                    lastTs, firstTs, data.size(),
                    minTsTime, minTs,
                    config.instId(), config.limit(), Instant.ofEpochMilli(config.startEpochMs()),
                    config.startEpochMs(), config.requestPauseMs(), config.pagesLimit());
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
            Thread.currentThread().interrupt(); // восстанавливаем флаг — цикл завершится на следующей итерации
            shuttingDown = true; // прерываем все циклы при остановке потока
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

