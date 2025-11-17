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
public class Candle1W extends AbstractCandle {

    private final static String NAME = "1w-candle";
    private static final Logger LOG = Logger.getLogger(Candle1W.class);

    protected final BufferRepository<CandlestickDto> candleBufferRepository;
    protected final CandleEventBus bus;
    protected final TimeSeriesBuffer<CandlestickDto> liveBuffer;
    protected final TimeSeriesBuffer<CandlestickDto> historicalBuffer;

    @Inject
    public Candle1W(ObjectMapper objectMapper, CandleEventBus bus) {
        this.bus = bus;
        this.liveBuffer = new TimeSeriesBuffer<>(300, CandleTimeframe.CANDLE_1W.getDuration());
        this.historicalBuffer = new TimeSeriesBuffer<>(100000, CandleTimeframe.CANDLE_1W.getDuration());
        this.candleBufferRepository = new CandleRepository();
    }

    @Override
    protected CandleTimeframe getCandleTimeframe() {
        return CandleTimeframe.CANDLE_1W;
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
    protected BufferRepository<CandlestickDto> getBufferRepository() {
        return candleBufferRepository;
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


