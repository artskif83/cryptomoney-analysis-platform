package artskif.trader.candle;

import artskif.trader.buffer.BufferedPoint;
import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.CandlestickHistoryDto;
import artskif.trader.dto.CandlestickPayloadDto;
import artskif.trader.events.candle.CandleEvent;
import artskif.trader.events.candle.CandleEventBus;
import artskif.trader.mapper.CandlestickMapper;
import artskif.trader.repository.BufferRepository;
import jakarta.enterprise.context.control.ActivateRequestContext;
import org.jboss.logging.Logger;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public abstract class AbstractCandle implements BufferedPoint<CandlestickDto> {

    protected static final String DEFAULT_SYMBOL = "BTC-USDT";

    // Конфигурация актуальности: запас времени в секундах для проверки последнего элемента
    protected static final long ACTUALITY_TIME_BUFFER_SECONDS = 10;

    // Минимальное количество элементов для считания буфера актуальным
    protected static final int MIN_BUFFER_SIZE_FOR_ACTUALITY = 1000;

    private final AtomicBoolean saveLiveEnabled = new AtomicBoolean(false);
    private final AtomicBoolean saveHistoricalEnabled = new AtomicBoolean(false);

    // Буферы и серии данных
    private final TimeSeriesBuffer<CandlestickDto> liveBuffer;
    private final TimeSeriesBuffer<CandlestickDto> historicalBuffer;
    private final BaseBarSeries liveBarSeries;
    private final BaseBarSeries historicalBarSeries;

    // ReadWriteLock для потокобезопасного доступа к серии баров
    private final ReadWriteLock liveSeriesLock = new ReentrantReadWriteLock();
    private final ReadWriteLock historicalSeriesLock = new ReentrantReadWriteLock();

    /**
     * Конструктор для инициализации буферов и серий данных
     *
     * @param name                    название инстанса
     * @param maxLiveBufferSize       максимальный размер live буфера
     * @param maxHistoricalBufferSize максимальный размер исторического буфера
     */
    protected AbstractCandle(String name, int maxLiveBufferSize, int maxHistoricalBufferSize) {
        this.liveBuffer = new TimeSeriesBuffer<>(maxLiveBufferSize);
        this.historicalBuffer = new TimeSeriesBuffer<>(maxHistoricalBufferSize);

        // Инициализация BaseBarSeries для live и historical данных
        this.liveBarSeries = new BaseBarSeriesBuilder()
                .withName(name + "_live")
                .withNumFactory(DecimalNumFactory.getInstance(6))
                .withMaxBarCount(maxLiveBufferSize)
                .build();

        this.historicalBarSeries = new BaseBarSeriesBuilder()
                .withName(name + "_historical")
                .withNumFactory(DecimalNumFactory.getInstance(6))
                .withMaxBarCount(maxHistoricalBufferSize)
                .build();
    }


    protected abstract BufferRepository<CandlestickDto> getBufferRepository();

    protected abstract CandleTimeframe getCandleTimeframe();

    public abstract String getName();

    public abstract Integer getMaxLiveBufferSize();

    public abstract Integer getMaxHistoryBufferSize();

    protected abstract CandleEventBus getEventBus();


    protected abstract Logger log();

    public BaseBarSeries getLiveBarSeries() {
        return liveBarSeries;
    }

    public BaseBarSeries getHistoricalBarSeries() {
        if (getHistoricalBuffer().isEmpty()) {
            initHistoricalData();
        }
        return historicalBarSeries;
    }

    /**
     * Инициализация исторических данных.
     * Вызывается по требованию, не при старте проекта.
     */
    @ActivateRequestContext
    protected void initHistoricalData() {
        log().infof("📚 [%s] Инициализация исторических данных для таймфрейма", getName());

        // Восстанавливаем Historical буфер из базы данных
        initRestoreHistoricalBuffer();

        // Заполняем Historical серию из Historical буфера
        copyHistoricalBufferToSeries();
    }

    public TimeSeriesBuffer<CandlestickDto> getLiveBuffer() {
        return liveBuffer;
    }

    public TimeSeriesBuffer<CandlestickDto> getHistoricalBuffer() {
        return historicalBuffer;
    }

    protected String getSymbol() {
        return DEFAULT_SYMBOL;
    }

    /**
     * Восстанавливает актуальный буфер из базы данных при старте проекта.
     * Вызывается автоматически при инициализации.
     */
    @ActivateRequestContext
    protected void initRestoreLiveBuffer() {
        log().debugf("💾 [%s] Восстанавливаем актуальный буфер из базы данных", getName());
        getLiveBuffer().putItems(getBufferRepository().restoreFromStorage(getMaxLiveBufferSize(), getCandleTimeframe(), getSymbol(), true));
        getLiveBuffer().incrementVersion();
    }

    /**
     * Восстанавливает исторический буфер из базы данных.
     * Вызывается по требованию, не при старте проекта.
     */
    @ActivateRequestContext
    protected void initRestoreHistoricalBuffer() {
        log().debugf("📥 [%s] Восстанавливаем исторический буфер из базы данных", getName());
        getHistoricalBuffer().putItems(getBufferRepository().restoreFromStorage(getMaxHistoryBufferSize(), getCandleTimeframe(), getSymbol(), false));
        getHistoricalBuffer().incrementVersion();
    }

    protected void initSaveLiveBuffer() {
        if (!isSaveLiveEnabled()) {
            log().debugf("💾 [%s] Активировано сохранение активного буфера по расписанию", getName());
        }
        saveLiveEnabled.set(true);
    }

    protected void initSaveHistoricalBuffer() {
        if (!isSaveHistoricalEnabled()) {
            log().debugf("💾 [%s] Активировано сохранение исторического буфера по расписанию", getName());
        }
        saveHistoricalEnabled.set(true);
    }

    public boolean isSaveLiveEnabled() {
        return saveLiveEnabled.get();
    }

    public boolean isSaveHistoricalEnabled() {
        return saveHistoricalEnabled.get();
    }

    @ActivateRequestContext
    public void saveBuffer() {
        log().infof("💾 [%s] Сохраняем информационные свечи в хранилище", getName());
        saveLiveBuffer();
        saveHistoricalBuffer();
    }

    @ActivateRequestContext
    public void saveLiveBuffer() {
        if (isSaveLiveEnabled()) {
            log().debugf("💾 [%s] Сохраняем в бд актуальный буфер", getName());
            Integer count = getBufferRepository().saveFromMap(getLiveBuffer().getDataMap());
            log().debugf("💾 [%s] Сохранен в бд актуальный буфер: %s записей", getName(), count);

            saveLiveEnabled.set(false);
        }
    }

    @ActivateRequestContext
    public void saveHistoricalBuffer() {
        if (isSaveHistoricalEnabled()) {
            log().debugf("💾 [%s] Сохраняем исторический буфер", getName());
            Integer count = getBufferRepository().saveFromMap(getHistoricalBuffer().getDataMap());
            log().debugf("💾 [%s] Сохранен в бд исторический буфер: %s записей", getName(), count);
            saveHistoricalEnabled.set(false);
        }
    }

    /**
     * Проверяет актуальность буфера по следующим критериям:
     * 1. Минимальное количество элементов в буфере
     * 2. Последовательность элементов (не должны отставать друг от друга больше чем на duration)
     * 3. Последний элемент не должен отставать от текущего времени больше чем на duration + запас (если checkLastElementActuality = true)
     *
     * @param buffer                    буфер для проверки
     * @param maxSize                   минимальный размер буфера (если null, проверка размера не выполняется)
     * @param checkLastElementActuality если true, проверяется актуальность последнего элемента относительно текущего времени
     * @param bufferName                название буфера для логирования
     */
    protected boolean isBufferActual(TimeSeriesBuffer<CandlestickDto> buffer, Integer maxSize, boolean checkLastElementActuality, String bufferName) {
        log().debugf("🔍 [%s] Проверяем актуальность буфера '%s'", getName(), bufferName);

        if (maxSize != null && buffer.size() < maxSize) {
            log().debugf("⚠️ [%s] Буфер '%s' не актуален: недостаточно элементов (%d < %d)",
                    getName(), bufferName, buffer.size(), buffer.getMaxSize());
            return false;
        }

        Instant now = Instant.now();
        Instant lastBucket = buffer.getLastBucket();

        if (lastBucket == null) {
            log().debugf("⚠️ [%s] Буфер '%s' не актуален: последний элемент не найден", getName(), bufferName);
            return false;
        }

        // Проверка актуальности последнего элемента (только если checkLastElementActuality = true)
        if (checkLastElementActuality) {
            long allowedDelaySeconds = (getCandleTimeframe().getDuration().toSeconds() * 2) + ACTUALITY_TIME_BUFFER_SECONDS;
            long actualDelaySeconds = now.getEpochSecond() - lastBucket.getEpochSecond();

            if (actualDelaySeconds > allowedDelaySeconds) {
                log().debugf("⚠️ [%s] Буфер '%s' не актуален: последний элемент слишком старый (задержка %d сек > допустимо %d сек)",
                        getName(), bufferName, actualDelaySeconds, allowedDelaySeconds);
                return false;
            }
        }

        // Проверка последовательности элементов
        long durationSeconds = getCandleTimeframe().getDuration().toSeconds();
        Instant previousBucket = null;

        for (Instant bucket : buffer.getDataMap().keySet()) {
            if (previousBucket != null) {
                long gap = bucket.getEpochSecond() - previousBucket.getEpochSecond();
                if (gap > durationSeconds * 2) { // допускаем пропуск максимум одной свечи
                    log().debugf("⚠️ [%s] Буфер '%s' не актуален: найден разрыв в последовательности (разрыв %d сек > допустимо %d сек)",
                            getName(), bufferName, gap, durationSeconds * 2);
                    return false;
                }
            }
            previousBucket = bucket;
        }

        log().debugf("🔍 [%s] Буфер '%s' актуален: размер=%d, последний элемент %s",
                getName(), bufferName, buffer.size(), lastBucket);
        return true;
    }

    /**
     * Копирует новые элементы из буфера в серию.
     * Если серия пуста, копируются все элементы из буфера.
     * Если серия не пуста, копируются только элементы, идущие после последнего элемента в серии.
     *
     * @param buffer     буфер-источник данных
     * @param series     серия-приемник данных
     * @param lock       блокировка для потокобезопасного доступа к серии
     * @param seriesName название серии для логирования
     */
    private void copyBufferToSeries(TimeSeriesBuffer<CandlestickDto> buffer,
                                    BaseBarSeries series,
                                    ReadWriteLock lock,
                                    String seriesName) {
        log().debugf("🔄 [%s] Начинаем копирование %s буфера в %s серию", getName(), seriesName, seriesName);

        // Получаем элементы для копирования вне блокировки
        Map<Instant, CandlestickDto> itemsToCopy;

        lock.writeLock().lock();
        try {
            Instant lastSeriesTimestamp = null;
            if (!series.isEmpty()) {
                lastSeriesTimestamp = series.getLastBar().getBeginTime();
                log().debugf("🔍 [%s] Последний элемент в %s серии: timestamp=%s",
                        getName(), seriesName, lastSeriesTimestamp);
            } else {
                log().debugf("🔍 [%s] %s серия пуста, будут скопированы все элементы",
                        getName(), seriesName);
            }

            // Получаем элементы для копирования
            if (lastSeriesTimestamp == null) {
                // Серия пуста - копируем все элементы
                itemsToCopy = buffer.getAll();
            } else {
                // Серия не пуста - копируем только элементы после последнего
                itemsToCopy = buffer.getItemsBetween(lastSeriesTimestamp, null);
            }

            if (itemsToCopy.isEmpty()) {
                log().debugf("ℹ️ [%s] Нет новых элементов для копирования в %s серию", getName(), seriesName);
                return;
            }

            // Копируем элементы (все под одной блокировкой)
            int count = 0;
            for (CandlestickDto candleDto : itemsToCopy.values()) {
                if (addBarToSeriesUnsafe(candleDto, series, seriesName)) {
                    count++;
                } else {
                    break;
                }
            }
            if (count > 0) {
                log().infof("✅ [%s] %s буфер скопирован в %s серию: %d новых элементов (всего в серии: %d)",
                        getName(), seriesName, seriesName, count, series.getBarCount());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Копирует новые элементы из LiveBuffer в LiveSeries.
     * Если LiveSeries пуста, копируются все элементы из LiveBuffer.
     * Если нет, то только те, что идут после последнего элемента в LiveSeries.
     * Вызывается автоматически при старте проекта.
     */
    protected void copyLiveBufferToSeries() {
        if (isBufferActual(getLiveBuffer(), getMaxLiveBufferSize(), true, "live")) {
            copyBufferToSeries(liveBuffer, liveBarSeries, liveSeriesLock, "live");
        } else {
            log().warnf("⚠️ [%s] Актуальный буфер не скопирован в live серию, т.к. буфер еще не актуален (размер: %d/%d)", getName(), getLiveBuffer().size(), getMaxLiveBufferSize());
        }
    }

    /**
     * Копирует новые элементы из HistoricalBuffer в HistoricalSeries.
     * Если HistoricalSeries пуста, копируются все элементы из HistoricalBuffer.
     * Если нет, то только те, что идут после последнего элемента в HistoricalSeries.
     * Вызывается по требованию, не при старте проекта.
     */
    protected void copyHistoricalBufferToSeries() {
        if (isBufferActual(getHistoricalBuffer(), null, false, "historical")) {
            copyBufferToSeries(historicalBuffer, historicalBarSeries, historicalSeriesLock, "historical");
        } else {
            log().warnf("⚠️ [%s] Исторический буфер не скопирован в historical серию, т.к. буфер еще не актуален", getName());
        }
    }

    /**
     * Добавляет новый бар в указанную серию (без блокировок, небезопасный метод).
     * Используется внутри методов, которые уже держат блокировку.
     *
     * @return true если бар был добавлен, false если был пропущен
     */
    private boolean addBarToSeriesUnsafe(CandlestickDto candlestickDto, BaseBarSeries series, String seriesType) {
        Bar bar = CandlestickMapper.mapDtoToBar(candlestickDto);
        if (bar == null) {
            return false;
        }

        // Проверяем, что новый бар идет после последнего бара в серии
        if (!series.isEmpty()) {
            Bar lastBar = series.getLastBar();
            long expectedTimestamp = lastBar.getEndTime().getEpochSecond() + getCandleTimeframe().getDuration().getSeconds();
            long actualTimestamp = bar.getEndTime().getEpochSecond();

            if (actualTimestamp != expectedTimestamp) {
                log().warnf("⚠️ [%s] Попытка добавить бар с timestamp=%s(%s сек), который меньше ожидаемого timestamp=%s(%s сек) в %s серию. Бар пропущен.",
                        getName(), java.time.Instant.ofEpochSecond(actualTimestamp), actualTimestamp, java.time.Instant.ofEpochSecond(expectedTimestamp), expectedTimestamp, seriesType);
                return false;
            }
        }
        series.addBar(bar);
        return true;
    }

    /**
     * Добавляет новый бар в указанную серию (с блокировкой, потокобезопасный метод)
     *
     * @return
     */
    private boolean addBarToSeries(CandlestickDto candlestickDto, BaseBarSeries series, ReadWriteLock lock, String seriesType) {
        lock.writeLock().lock();
        try {
            return addBarToSeriesUnsafe(candlestickDto, series, seriesType);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Добавляет новый бар в live серию
     *
     * @return
     */
    protected boolean addBarToLiveSeries(CandlestickDto candlestickDto) {
        return addBarToSeries(candlestickDto, liveBarSeries, liveSeriesLock, "live");
    }


    /**
     * Восстанавливает буфер из пачки истории (JSON-массив /history-*-candles).
     * message: строка массива data, например:
     * [[1698796800000,"34300","34500","34000","34210",...], [...], ...]
     */
    public void restoreFromHistory(String message) {
        try {
            CandlestickHistoryDto historyDto = CandlestickMapper.mapJsonMessageToCandlestickMap(message, getCandleTimeframe());

            if (historyDto.getData().isEmpty()) {
                log().warnf("⚠️ [%s] После парсинга история пуста", getName());
                return;
            }

            log().infof("📚 [%s] Восстанавливаем %d свечей (instId=%s, isLast=%s, первый=%s, последний=%s)",
                    getName(), historyDto.getData().size(), historyDto.getInstId(), historyDto.isLast(),
                    historyDto.getData().isEmpty() ? "N/A" : historyDto.getData().keySet().stream().min(Instant::compareTo).orElse(null),
                    historyDto.getData().isEmpty() ? "N/A" : historyDto.getData().keySet().stream().max(Instant::compareTo).orElse(null));

            getLiveBuffer().putItems(historyDto.getData());
            getLiveBuffer().incrementVersion();
            if (historyDto.getData().size() >= getMaxHistoryBufferSize()) {
                log().warnf("⚠️ [%s] Размер исторических данных (%d) превышает максимальный размер буфера (%d). Добавьте аксимальный размер исторического буфера, иначе данные будут обрезаны",
                        getName(), historyDto.getData().size(), getMaxHistoryBufferSize());
            }
            getHistoricalBuffer().putItems(historyDto.getData());
            getHistoricalBuffer().incrementVersion();

            copyLiveBufferToSeries();

            initSaveHistoricalBuffer();
            getEventBus().publish(new CandleEvent(CandleEventType.CANDLE_HISTORY, getCandleTimeframe(), historyDto.getInstId(), null, null, null, false));
        } catch (Exception e) {
            log().errorf(e, "❌ [%s] Не удалось обработать элементы для истории: %s", getName(), e.getMessage());
        }
    }

    public void handleTick(String message) {
        try {
            CandlestickPayloadDto candlestickPayloadDto;
            Optional<CandlestickPayloadDto> opt = CandlestickMapper.map(message, getCandleTimeframe());
            if (opt.isPresent()) {
                candlestickPayloadDto = opt.get();
            } else {
                return;
            }

            CandlestickDto candle = candlestickPayloadDto.getCandle();

            Instant bucket = candle.getTimestamp();
            // Если новый тик принадлежит новой свече — подтвердить предыдущую
            if (Boolean.TRUE.equals(candle.getConfirmed())) {
                log().debugf("🕯️ [%s] Получена подтвержденная свеча: bucket=%s, o=%s, h=%s, l=%s, c=%s, v=%s",
                        getName(), bucket, candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose(), candle.getVolume());

                if (liveBarSeries.getBarCount() < getMaxLiveBufferSize()) {
                    log().debugf("⏳ [%s] Live серия еще не заполнена: %d/%d элементов. Ожидаем добавления элементов",
                            getName(), liveBarSeries.getBarCount(), getMaxLiveBufferSize());
                    return;
                }

                // Добавляем в буферы
                getLiveBuffer().putItem(bucket, candle);
                getLiveBuffer().incrementVersion();

                // Проверяем актуальность буферов и добавляем в серии (версия не инкрементится)
                if (isBufferActual(getLiveBuffer(), getMaxLiveBufferSize(), true, "live candle") &&
                        addBarToLiveSeries(candle)) {
                    initSaveLiveBuffer();
                    getEventBus().publish(new CandleEvent(CandleEventType.CANDLE_TICK, getCandleTimeframe(), candlestickPayloadDto.getInstrumentId(), bucket, candle, candle.getConfirmed(), false));
                    log().infof("✅ [%s] Свеча успешно добавлена в live серию: bucket=%s, close=%s", getName(), bucket, candle.getClose());

                } else {
                    log().warnf("⚠️ [%s] Свеча не добавлена в live серию, т.к. буфер еще не актуален или свечи дублируются", getName());
                }
            }
        } catch (Exception e) {
            log().errorf(e, "❌ [%s] Не удалось разобрать сообщение - %s. Ошибка - %s", getName(), message, e.getMessage());
        }
    }

}
