package artskif.trader.candle;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.candle.CandleEventBus;
import artskif.trader.repository.BufferRepository;
import jakarta.enterprise.context.control.ActivateRequestContext;
import org.jboss.logging.Logger;

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


    public CandleInstance(CandleTimeframe timeframe, String name,
                          int maxLiveBufferSize, int maxHistoricalBufferSize, CandleEventBus bus,
                          BufferRepository<CandlestickDto> candleBufferRepository) {
        super(name, maxLiveBufferSize, maxHistoricalBufferSize);
        this.timeframe = timeframe;
        this.name = name;
        this.maxLiveBufferSize = maxLiveBufferSize;
        this.maxHistoricalBufferSize = maxHistoricalBufferSize;
        this.bus = bus;
        this.logger = Logger.getLogger(Candle.class.getName() + "." + name);
        this.candleBufferRepository = candleBufferRepository;
    }

    @ActivateRequestContext
    public void initLiveData() {
        logger.infof("🔌 [%s] Инициализация инстанса свечей для таймфрейма", name);

        // Восстанавливаем только Live буфер при старте
        initRestoreLiveBuffer();

        // Заполняем только Live серию из Live буфера при старте
        copyLiveBufferToSeries();
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
}

