package artskif.trader.candle;

import artskif.trader.buffer.BufferedPoint;
import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.CandlestickHistoryDto;
import artskif.trader.dto.CandlestickPayloadDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.events.CandleEventType;
import artskif.trader.mapper.CandlestickMapper;
import artskif.trader.repository.BufferRepository;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBarSeries;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;


public abstract class AbstractCandle implements BufferedPoint<CandlestickDto> {

    protected static final String DEFAULT_SYMBOL = "BTC-USDT";

    // Конфигурация актуальности: запас времени в секундах для проверки последнего элемента
    protected static final long ACTUALITY_TIME_BUFFER_SECONDS = 10;

    // Минимальное количество элементов для считания буфера актуальным
    protected static final int MIN_BUFFER_SIZE_FOR_ACTUALITY = 1000;

    private final AtomicBoolean saveLiveEnabled = new AtomicBoolean(false);
    private final AtomicBoolean saveHistoricalEnabled = new AtomicBoolean(false);

    protected abstract BufferRepository<CandlestickDto> getBufferRepository();

    protected abstract CandleTimeframe getCandleTimeframe();

    public abstract String getName();

    public abstract Integer getMaxLiveBufferSize();

    public abstract Integer getMaxHistoryBufferSize();

    protected abstract CandleEventBus getEventBus();


    protected abstract Logger log();

    protected abstract BaseBarSeries getLiveBarSeries();

    protected abstract BaseBarSeries getHistoricalBarSeries();

    protected abstract ReadWriteLock getLiveSeriesLock();

    protected abstract ReadWriteLock getHistoricalSeriesLock();

    protected String getSymbol() {
        return DEFAULT_SYMBOL;
    }

    /**
     * Восстанавливает актуальный буфер из базы данных при старте проекта.
     * Вызывается автоматически при инициализации.
     */
    @ActivateRequestContext
    protected void initRestoreLiveBuffer() {
        log().infof("📥 [%s] Восстанавливаем актуальный буфер из базы данных", getName());
        getLiveBuffer().putItems(getBufferRepository().restoreFromStorage(getMaxLiveBufferSize(), getCandleTimeframe(), getSymbol(), true));
        getLiveBuffer().incrementVersion();
    }

    /**
     * Восстанавливает исторический буфер из базы данных.
     * Вызывается по требованию, не при старте проекта.
     */
    @ActivateRequestContext
    protected void initRestoreHistoricalBuffer() {
        log().infof("📥 [%s] Восстанавливаем исторический буфер из базы данных", getName());
        getHistoricalBuffer().putItems(getBufferRepository().restoreFromStorage(getMaxHistoryBufferSize(), getCandleTimeframe(), getSymbol(), false));
        getHistoricalBuffer().incrementVersion();
    }

    protected void initSaveLiveBuffer() {
        if (!isSaveLiveEnabled()) {
            log().infof("📥 [%s] Активировано сохранение активного буфера по расписанию", getName());
        }
        saveLiveEnabled.set(true);
    }

    protected void initSaveHistoricalBuffer() {
        if (!isSaveHistoricalEnabled()) {
            log().infof("📥 [%s] Активировано сохранение исторического буфера по расписанию", getName());
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
    protected void saveLiveBuffer() {
        if (isSaveLiveEnabled()) {
            log().debugf("💾 [%s] Сохраняем в бд актуальный буфер", getName());
            Integer count = getBufferRepository().saveFromMap(getLiveBuffer().getDataMap());
            log().debugf("💾 [%s] Сохранен в бд актуальный буфер: %s записей", getName(), count);

            saveLiveEnabled.set(false);
        }
    }

    @ActivateRequestContext
    protected void saveHistoricalBuffer() {
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
     * 3. Последний элемент не должен отставать от текущего времени больше чем на duration + запас
     */
    protected boolean isBufferActual(TimeSeriesBuffer<CandlestickDto> buffer, Integer maxSize) {
        if (maxSize != null && buffer.size() < maxSize) {
            log().debugf("⚠️ [%s] Буфер не актуален: недостаточно элементов (%d < %d)",
                    getName(), buffer.size(), buffer.getMaxSize());
            return false;
        }

        Instant now = Instant.now();
        Instant lastBucket = buffer.getLastBucket();

        if (lastBucket == null) {
            log().debugf("⚠️ [%s] Буфер не актуален: последний элемент не найден", getName());
            return false;
        }

        // Проверка актуальности последнего элемента
        long allowedDelaySeconds = (getCandleTimeframe().getDuration().toSeconds() * 2) + ACTUALITY_TIME_BUFFER_SECONDS;
        long actualDelaySeconds = now.getEpochSecond() - lastBucket.getEpochSecond();

        if (actualDelaySeconds > allowedDelaySeconds) {
            log().debugf("⚠️ [%s] Буфер не актуален: последний элемент слишком старый (задержка %d сек > допустимо %d сек)",
                    getName(), actualDelaySeconds, allowedDelaySeconds);
            return false;
        }

        // Проверка последовательности элементов
        long durationSeconds = getCandleTimeframe().getDuration().toSeconds();
        Instant previousBucket = null;

        for (Instant bucket : buffer.getDataMap().keySet()) {
            if (previousBucket != null) {
                long gap = bucket.getEpochSecond() - previousBucket.getEpochSecond();
                if (gap > durationSeconds * 2) { // допускаем пропуск максимум одной свечи
                    log().debugf("⚠️ [%s] Буфер не актуален: найден разрыв в последовательности (разрыв %d сек > допустимо %d сек)",
                            getName(), gap, durationSeconds * 2);
                    return false;
                }
            }
            previousBucket = bucket;
        }

        log().debugf("✅ [%s] Буфер актуален: размер=%d, последний элемент %s",
                getName(), buffer.size(), lastBucket);
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
        log().infof("🔄 [%s] Начинаем копирование %s буфера в %s серию", getName(), seriesName, seriesName);

        Instant lastSeriesTimestamp = null;
        lock.readLock().lock();
        try {
            if (!series.isEmpty()) {
                lastSeriesTimestamp = series.getLastBar().getEndTime();
                log().debugf("🔍 [%s] Последний элемент в %s серии: timestamp=%s",
                        getName(), seriesName, lastSeriesTimestamp);
            } else {
                log().debugf("🔍 [%s] %s серия пуста, будут скопированы все элементы",
                        getName(), seriesName);
            }
        } finally {
            lock.readLock().unlock();
        }

        // Получаем элементы для копирования
        Map<Instant, CandlestickDto> itemsToCopy;
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

        // Копируем элементы
        int count = 0;
        for (CandlestickDto candleDto : itemsToCopy.values()) {
            addBarToSeries(candleDto, series, lock, seriesName);
            count++;
        }

        lock.readLock().lock();
        try {
            log().infof("✅ [%s] %s буфер скопирован в %s серию: %d новых элементов (всего в серии: %d)",
                    getName(), seriesName, seriesName, count, series.getBarCount());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Копирует новые элементы из LiveBuffer в LiveSeries.
     * Если LiveSeries пуста, копируются все элементы из LiveBuffer.
     * Если нет, то только те, что идут после последнего элемента в LiveSeries.
     * Вызывается автоматически при старте проекта.
     */
    protected void copyLiveBufferToSeries() {
        if (isBufferActual(getLiveBuffer(), getMaxLiveBufferSize())) {
            copyBufferToSeries(getLiveBuffer(), getLiveBarSeries(), getLiveSeriesLock(), "live");
        } else {
            log().warnf("⚠️ [%s] Актуальный буфер не скопирован в live серию, т.к. буфер еще не актуален", getName());
        }
    }

    /**
     * Копирует новые элементы из HistoricalBuffer в HistoricalSeries.
     * Если HistoricalSeries пуста, копируются все элементы из HistoricalBuffer.
     * Если нет, то только те, что идут после последнего элемента в HistoricalSeries.
     * Вызывается по требованию, не при старте проекта.
     */
    protected void copyHistoricalBufferToSeries() {
        if (isBufferActual(getHistoricalBuffer(), null)) {
            copyBufferToSeries(getHistoricalBuffer(), getHistoricalBarSeries(), getHistoricalSeriesLock(), "historical");
        } else {
            log().warnf("⚠️ [%s] Исторический буфер не скопирован в historical серию, т.к. буфер еще не актуален", getName());
        }
    }

    /**
     * Добавляет новый бар в указанную серию
     */
    private void addBarToSeries(CandlestickDto candlestickDto, BaseBarSeries series, ReadWriteLock lock, String seriesType) {
        Bar bar = CandlestickMapper.mapDtoToBar(candlestickDto);
        if (bar == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            // Проверяем, что новый бар идет после последнего бара в серии
            if (!series.isEmpty()) {
                Bar lastBar = series.getLastBar();
                long expectedTimestamp = lastBar.getEndTime().getEpochSecond() + getCandleTimeframe().getDuration().getSeconds();
                long actualTimestamp = bar.getEndTime().getEpochSecond();

                if (actualTimestamp != expectedTimestamp) {
                    log().warnf("⚠️ [%s] Попытка добавить бар с timestamp=%s, который меньше ожидаемого timestamp=%s в %s серию. Бар пропущен.",
                            getName(), candlestickDto.getTimestamp(), java.time.Instant.ofEpochSecond(expectedTimestamp), seriesType);
                    return;
                }
            }
            series.addBar(bar);
            log().tracef("🔹 [%s] Добавлен бар в %s серию: timestamp=%s", getName(), seriesType, candlestickDto.getTimestamp());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Добавляет новый бар в live серию
     */
    protected void addBarToLiveSeries(CandlestickDto candlestickDto) {
        addBarToSeries(candlestickDto, getLiveBarSeries(), getLiveSeriesLock(), "live");
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

            getLiveBuffer().putItems(historyDto.getData());
            getLiveBuffer().incrementVersion();
            log().infof("✅ [%s] В актуальный буфер пришло %d элементов. Текущий размер %d (instId=%s, isLast=%s)",
                    getName(), historyDto.getData().size(), getLiveBuffer().size(), historyDto.getInstId(), historyDto.isLast());

            getHistoricalBuffer().putItems(historyDto.getData());
            getHistoricalBuffer().incrementVersion();
            log().infof("✅ [%s] В исторический буфер пришло %d элементов. Текущий размер %d (instId=%s, isLast=%s)",
                    getName(), historyDto.getData().size(), getHistoricalBuffer().size(), historyDto.getInstId(), historyDto.isLast());

            copyLiveBufferToSeries();

            initSaveHistoricalBuffer();
            getEventBus().publish(new CandleEvent(CandleEventType.CANDLE_HISTORY, getCandleTimeframe(), historyDto.getInstId(), null, null, null));
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

                // Добавляем в буферы
                getLiveBuffer().putItem(bucket, candle);
                getLiveBuffer().incrementVersion();

                // Проверяем актуальность буферов и добавляем в серии (версия не инкрементится)
                if (isBufferActual(getLiveBuffer(), getMaxLiveBufferSize()) && getLiveBarSeries().getBarCount() >= getMaxLiveBufferSize()) {
                    addBarToLiveSeries(candle);
                    initSaveLiveBuffer();
                } else {
                    log().warnf("⚠️ [%s] Свеча не добавлена в live серию, т.к. буфер еще не актуален", getName());
                }


                getEventBus().publish(new CandleEvent(CandleEventType.CANDLE_TICK, getCandleTimeframe(), candlestickPayloadDto.getInstrumentId(), bucket, candle, candle.getConfirmed()));

            }
        } catch (Exception e) {
            log().errorf(e, "❌ [%s] Не удалось разобрать сообщение - %s. Ошибка - %s", getName(), message, e.getMessage());
        }
    }

}
