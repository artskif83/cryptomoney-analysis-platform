package artskif.trader.candle;

import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEventBus;
import artskif.trader.repository.BufferRepository;
import artskif.trader.repository.CandleRepository;
import jakarta.enterprise.context.control.ActivateRequestContext;
import org.jboss.logging.Logger;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Класс, представляющий экземпляр свечи для конкретного таймфрейма.
 * Содержит собственные буферы и логику обработки.
 * Поддерживает потокобезопасный доступ к liveBarSeries и historicalBarSeries.
 */
public class CandleInstance extends AbstractCandle {

    private final int maxLiveBufferSize;
    private final int maxHistoricalBufferSize;

    private final CandleTimeframe timeframe;
    private final String name;
    private final CandleEventBus bus;
    private final Logger logger;

    private final BufferRepository<CandlestickDto> candleBufferRepository;
    private final TimeSeriesBuffer<CandlestickDto> liveBuffer;
    private final TimeSeriesBuffer<CandlestickDto> historicalBuffer;

    private final BaseBarSeries liveBarSeries;
    private final BaseBarSeries historicalBarSeries;

    // ReadWriteLock для потокобезопасного доступа к серии баров
    private final ReadWriteLock liveSeriesLock = new ReentrantReadWriteLock();
    private final ReadWriteLock historicalSeriesLock = new ReentrantReadWriteLock();


    public CandleInstance(CandleTimeframe timeframe, String name,
                          int maxLiveBufferSize, int maxHistoricalBufferSize, CandleEventBus bus) {
        this.timeframe = timeframe;
        this.name = name;
        this.maxLiveBufferSize = maxLiveBufferSize;
        this.maxHistoricalBufferSize = maxHistoricalBufferSize;
        this.bus = bus;
        this.logger = Logger.getLogger(Candle.class.getName() + "." + name);

        this.liveBuffer = new TimeSeriesBuffer<>(maxLiveBufferSize);
        this.historicalBuffer = new TimeSeriesBuffer<>(maxHistoricalBufferSize);
        this.candleBufferRepository = new CandleRepository();

        // Инициализация BaseBarSeries для live и historical данных
        this.liveBarSeries = new BaseBarSeriesBuilder()
                .withName(name + "_live")
                .withNumFactory(DecimalNumFactory.getInstance(2))
                .withMaxBarCount(maxLiveBufferSize)
                .build();

        this.historicalBarSeries = new BaseBarSeriesBuilder()
                .withName(name + "_historical")
                .withNumFactory(DecimalNumFactory.getInstance(2))
                .withMaxBarCount(maxHistoricalBufferSize)
                .build();
    }

    @ActivateRequestContext
    public void init() {
        logger.infof("✅ [%s] Инициализация инстанса свечей для таймфрейма", name);

        // Восстанавливаем только Live буфер при старте
        initRestoreLiveBuffer();

        // Заполняем только Live серию из Live буфера при старте
        copyLiveBufferToSeries();
    }

    /**
     * Инициализация исторических данных.
     * Вызывается по требованию, не при старте проекта.
     */
    @ActivateRequestContext
    public void initHistoricalData() {
        logger.infof("📚 [%s] Инициализация исторических данных для таймфрейма", name);

        // Восстанавливаем Historical буфер из базы данных
        initRestoreHistoricalBuffer();

        // Заполняем Historical серию из Historical буфера
        copyHistoricalBufferToSeries();
    }

    @Override
    protected BufferRepository<CandlestickDto> getBufferRepository() {
        return candleBufferRepository;
    }

    @Override
    protected CandleTimeframe getCandleTimeframe() {
        return timeframe;
    }

    @Override
    public TimeSeriesBuffer<CandlestickDto> getLiveBuffer() {
        return liveBuffer;
    }

    @Override
    public TimeSeriesBuffer<CandlestickDto> getHistoricalBuffer() {
        return historicalBuffer;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Integer getMaxLiveBufferSize() {
        return maxLiveBufferSize;
    }

    @Override
    public Integer getMaxHistoryBufferSize() {
        return maxHistoricalBufferSize;
    }


    @Override
    protected CandleEventBus getEventBus() {
        return bus;
    }

    @Override
    public Logger log() {
        return logger;
    }

    @Override
    protected BaseBarSeries getLiveBarSeries() {
        return liveBarSeries;
    }

    @Override
    protected BaseBarSeries getHistoricalBarSeries() {
        return historicalBarSeries;
    }

    @Override
    protected ReadWriteLock getLiveSeriesLock() {
        return liveSeriesLock;
    }

    @Override
    protected ReadWriteLock getHistoricalSeriesLock() {
        return historicalSeriesLock;
    }
}

