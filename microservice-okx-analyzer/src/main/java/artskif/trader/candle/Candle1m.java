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
public class Candle1m extends AbstractCandle {

    private final static String NAME = "1m-candle";
    private static final Logger LOG = Logger.getLogger(Candle1m.class);

    protected final BufferRepository<CandlestickDto> candleBufferRepository;
    protected final CandleEventBus bus;
    protected final TimeSeriesBuffer<CandlestickDto> timeSeriesBuffer;

    @Inject
    public Candle1m(ObjectMapper objectMapper, CandleEventBus bus) {
        this.bus = bus;
        this.timeSeriesBuffer = new TimeSeriesBuffer<>(500, CandleTimeframe.CANDLE_1M.getDuration());
        this.candleBufferRepository = new CandleRepository();
    }


    @Override
    protected BufferRepository<CandlestickDto> getBufferRepository() {
        return candleBufferRepository;
    }

    @Override
    protected CandleTimeframe getCandleTimeframe() {
        return CandleTimeframe.CANDLE_1M;
    }

    @Override
    public TimeSeriesBuffer<CandlestickDto> getBuffer() {
        return timeSeriesBuffer;
    }

    @Override
    public String getName() {
        return NAME;
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
