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
public class Candle1D extends AbstractCandle {

    private final static String NAME = "1D-candle";
    private static final Logger LOG = Logger.getLogger(Candle1D.class);

    protected final BufferRepository<CandlestickDto> candleBufferRepository;
    protected final CandleEventBus bus;
    protected final TimeSeriesBuffer<CandlestickDto> timeSeriesBuffer;

    @Inject
    public Candle1D(ObjectMapper objectMapper, CandleEventBus bus) {
        this.bus = bus;
        this.timeSeriesBuffer = new TimeSeriesBuffer<>(1000000, CandleTimeframe.CANDLE_1D.getDuration());
        this.candleBufferRepository = new CandleRepository();
    }

    @Override
    protected CandleTimeframe getCandleTimeframe() {
        return CandleTimeframe.CANDLE_1D;
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
