package artskif.trader.indicator.rsi;

import artskif.trader.candle.Candle1m;
import artskif.trader.candle.CandleType;
import artskif.trader.common.Buffer;
import artskif.trader.common.BufferRepository;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.AbstractIndicator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Startup
@ApplicationScoped
@NoArgsConstructor(force = true)
public class RsiIndicator1m  extends AbstractIndicator<RsiPoint> {

    BufferRepository<RsiPoint> rsiBufferRepository;
    Candle1m candle1m;

    @Inject
    public RsiIndicator1m(ObjectMapper objectMapper, Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.rsiBufferRepository = new BufferRepository<>(objectMapper, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, RsiPoint.class));
        this.candle1m = candle1m;
    }

    private final Buffer<RsiPoint> buffer = new Buffer<>(Duration.ofMinutes(1), 100);
    private final Path pathForSave = Paths.get("rsiIndicator1m.json");

    @Override
    protected CandleType getCandleType() {
        return CandleType.CANDLE_1M;
    }

    @Override
    protected void process(CandleEvent ev) {
        CandlestickDto c = ev.candle();
        Instant bucket = ev.bucket();
        Map<Instant, CandlestickDto> history = candle1m.getBuffer().getSnapshot();

        Optional<RsiPoint> point = RsiCalculator.computeLastRsi(history, true);
        point.ifPresent(p -> buffer.putItem(bucket, p));

        System.out.println("📥 [" + getName() + "] Получено новое значение  RSI - " + point.orElse(null));

        if (Boolean.TRUE.equals(c.getConfirmed())) {
            saveBuffer();
        }
    }

    @Override
    public Buffer<RsiPoint> getBuffer() {
        return buffer;
    }

    @Override
    public String getName() {
        return "1m-RSI";
    }

    @Override
    public Path getPathForSave() {
        return pathForSave;
    }

    @Override
    public BufferRepository<RsiPoint> getBufferRepository() {
        return rsiBufferRepository;
    }
}
