package artskif.trader.candle;

import artskif.trader.buffer.Buffer;
import artskif.trader.buffer.BufferRepository;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEventBus;
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
public class Candle4H extends AbstractTimeSeriesTicker {

    private final static String NAME = "4H-candle";
    private static final Logger LOG = Logger.getLogger(Candle4H.class);

    protected final BufferRepository<CandlestickDto> candleBufferRepository;
    protected final CandleEventBus bus;
    protected final Buffer<CandlestickDto> buffer;
    protected final Path pathForSave = Paths.get("candles4h.json");

    @Inject
    public Candle4H(ObjectMapper objectMapper, CandleEventBus bus) {
        this.bus = bus;
        this.buffer = new Buffer<>(NAME, Duration.ofHours(4), 300);
        this.candleBufferRepository = new BufferRepository<>(objectMapper, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, CandlestickDto.class));
    }

    @Override
    protected CandleTimeframe getCandleTimeframe() {
        return CandleTimeframe.CANDLE_4H;
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
    public Path getPathForSave() {
        return pathForSave;
    }

    @Override
    public BufferRepository<CandlestickDto> getBufferRepository() {
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
