package artskif.trader.candle;

import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEventBus;
import artskif.trader.repository.BufferRepository;
import artskif.trader.repository.CandleRepository;
import jakarta.enterprise.context.control.ActivateRequestContext;
import org.jboss.logging.Logger;

/**
 * Класс, представляющий экземпляр свечи для конкретного таймфрейма.
 * Содержит собственные буферы и логику обработки.
 */
public class CandleInstance extends AbstractCandle {

    private static final int MAX_LIVE_BUFFER_SIZE = 50;
    private static final int MAX_HISTORICAL_BUFFER_SIZE = 1000000;

    private final CandleTimeframe timeframe;
    private final String name;
    private final boolean enabled;
    private final CandleEventBus bus;
    private final Logger logger;

    private final BufferRepository<CandlestickDto> candleBufferRepository;
    private final TimeSeriesBuffer<CandlestickDto> liveBuffer;
    private final TimeSeriesBuffer<CandlestickDto> historicalBuffer;

    public CandleInstance(CandleTimeframe timeframe, String name, boolean enabled, CandleEventBus bus) {
        this.timeframe = timeframe;
        this.name = name;
        this.enabled = enabled;
        this.bus = bus;
        this.logger = Logger.getLogger(Candle.class.getName() + "." + name);

        this.liveBuffer = new TimeSeriesBuffer<>(MAX_LIVE_BUFFER_SIZE);
        this.historicalBuffer = new TimeSeriesBuffer<>(MAX_HISTORICAL_BUFFER_SIZE);
        this.candleBufferRepository = new CandleRepository();
    }

    @ActivateRequestContext
    public void init() {
        if (!enabled) {
            logger.infof("⚠️ [%s] Таймфрейм отключен", name);
            return;
        }
        logger.infof("✅ [%s] Инициализация таймфрейма", name);
        initRestoreBuffer();
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
        return MAX_LIVE_BUFFER_SIZE;
    }

    @Override
    public Integer getMaxHistoryBufferSize() {
        return MAX_HISTORICAL_BUFFER_SIZE;
    }

    @Override
    public boolean getEnabled() {
        return enabled;
    }

    @Override
    protected CandleEventBus getEventBus() {
        return bus;
    }

    @Override
    public Logger log() {
        return logger;
    }
}

