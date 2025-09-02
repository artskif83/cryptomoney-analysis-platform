package artskif.trader.candle;

import artskif.trader.common.Buffer;
import artskif.trader.common.BufferRepository;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;

@ApplicationScoped
public class Candle1H extends AbstractTimeSeriesTicker {

    protected final BufferRepository<CandlestickDto> candleBufferRepository;
    protected final CandleEventBus bus;
    protected final Buffer<CandlestickDto> buffer = new Buffer<>(Duration.ofHours(1), 300);
    protected final Path pathForSave = Paths.get("candles1h.json");

    @Inject
    public Candle1H(ObjectMapper objectMapper, CandleEventBus bus) {
        this.bus = bus;
        this.candleBufferRepository = new BufferRepository<>(objectMapper, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, CandlestickDto.class));
    }

    @Override
    protected CandleType getCandleType() {
        return CandleType.CANDLE_1H;
    }

    @Override
    public Buffer<CandlestickDto> getBuffer() {
        return buffer;
    }

    @Override
    public String getName() {
        return "1H-candle";
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
}
