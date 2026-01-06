package artskif.trader.candle;

import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEventBus;
import artskif.trader.mapper.CandlestickMapper;
import artskif.trader.repository.BufferRepository;
import artskif.trader.repository.CandleRepository;
import jakarta.enterprise.context.control.ActivateRequestContext;
import lombok.Getter;
import org.jboss.logging.Logger;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;

/**
 * Класс, представляющий экземпляр свечи для конкретного таймфрейма.
 * Содержит собственные буферы и логику обработки.
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

    @Getter
    private final BaseBarSeries liveBarSeries;
    @Getter
    private final BaseBarSeries historicalBarSeries;

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
        initRestoreBuffer();

        // Заполняем BaseBarSeries из буферов после восстановления данных
        populateBarSeriesFromBuffers();
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

    /**
     * Добавляет новый бар в live серию
     */
    protected void addBarToLiveSeries(CandlestickDto candlestickDto) {
        Bar bar = CandlestickMapper.mapDtoToBar(candlestickDto);
        if (bar != null) {
            liveBarSeries.addBar(bar);
            logger.tracef("🔹 [%s] Добавлен бар в live серию: timestamp=%s", name, candlestickDto.getTimestamp());
        }
    }

    /**
     * Добавляет новый бар в historical серию
     */
    protected void addBarToHistoricalSeries(CandlestickDto candlestickDto) {
        Bar bar = CandlestickMapper.mapDtoToBar(candlestickDto);
        if (bar != null) {
            historicalBarSeries.addBar(bar);
            logger.tracef("🔹 [%s] Добавлен бар в historical серию: timestamp=%s", name, candlestickDto.getTimestamp());
        }
    }

    /**
     * Заполняет серии данными из буферов при инициализации
     */
    protected void populateBarSeriesFromBuffers() {
        // Заполняем historical серию из historical буфера
        for (CandlestickDto candlestickDto : historicalBuffer.getList()) {
            addBarToHistoricalSeries(candlestickDto);
        }
        logger.infof("✅ [%s] Historical серия заполнена: %d баров", name, historicalBarSeries.getBarCount());

        // Заполняем live серию из live буфера
        for (CandlestickDto candlestickDto : liveBuffer.getList()) {
            addBarToLiveSeries(candlestickDto);
        }
        logger.infof("✅ [%s] Live серия заполнена: %d баров", name, liveBarSeries.getBarCount());
    }
}

