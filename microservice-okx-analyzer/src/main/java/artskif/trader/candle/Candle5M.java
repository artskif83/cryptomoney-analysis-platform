package artskif.trader.candle;

import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEventBus;
import artskif.trader.repository.BufferRepository;
import artskif.trader.repository.CandleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class Candle5M extends AbstractCandle {

    private final static String NAME = "5m-candle";
    private static final Logger LOG = Logger.getLogger(Candle5M.class);
    private static final int MAX_LIVE_BUFFER_SIZE = 50;
    private static final int MAX_HISTORICAL_BUFFER_SIZE = 100000;

    protected final BufferRepository<CandlestickDto> candleBufferRepository;
    protected final CandleEventBus bus;
    protected final TimeSeriesBuffer<CandlestickDto> liveBuffer;
    protected final TimeSeriesBuffer<CandlestickDto> historicalBuffer;

    @Inject
    public Candle5M(ObjectMapper objectMapper, CandleEventBus bus) {
        this.bus = bus;
        this.liveBuffer = new TimeSeriesBuffer<>(MAX_LIVE_BUFFER_SIZE, CandleTimeframe.CANDLE_5M.getDuration());
        this.historicalBuffer = new TimeSeriesBuffer<>(MAX_HISTORICAL_BUFFER_SIZE, CandleTimeframe.CANDLE_5M.getDuration());
        this.candleBufferRepository = new CandleRepository();
    }


    @Override
    protected BufferRepository<CandlestickDto> getBufferRepository() {
        return candleBufferRepository;
    }

    @Override
    protected CandleTimeframe getCandleTimeframe() {
        return CandleTimeframe.CANDLE_5M;
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
        return NAME;
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
    protected CandleEventBus getEventBus() {
        return bus;
    }

    @Override
    public Logger log() {
        return LOG;
    }
}