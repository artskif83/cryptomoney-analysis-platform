package artskif.trader.candle;

import artskif.trader.buffer.Buffer;
import artskif.trader.buffer.BufferFileRepository;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEventBus;
import artskif.trader.repository.BufferRepository;
import artskif.trader.repository.CandleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;

@ApplicationScoped
public class Candle1H extends AbstractCandle {

    private final static String NAME = "1H-candle";
    private static final Logger LOG = Logger.getLogger(Candle1H.class);

    protected final BufferRepository<CandlestickDto> candleBufferRepository;
    protected final BufferFileRepository<CandlestickDto> candleBufferFileRepository;
    protected final CandleEventBus bus;
    protected final Buffer<CandlestickDto> buffer;

    @Inject
    public Candle1H(ObjectMapper objectMapper, CandleEventBus bus) {
        this.bus = bus;
        this.buffer = new Buffer<>(NAME, Duration.ofHours(1), 300);
        this.candleBufferFileRepository = new BufferFileRepository<>(objectMapper, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, CandlestickDto.class));
        this.candleBufferRepository = new CandleRepository();
    }


    @Override
    protected BufferRepository<CandlestickDto> getBufferRepository() {
        return candleBufferRepository;
    }

    @Override
    protected CandleTimeframe getCandleTimeframe() {
        return CandleTimeframe.CANDLE_1H;
    }

    @Override
    public Buffer<CandlestickDto> getBuffer() {
        return buffer;
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
