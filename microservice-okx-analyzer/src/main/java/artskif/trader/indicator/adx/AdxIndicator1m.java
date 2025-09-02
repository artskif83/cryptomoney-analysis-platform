package artskif.trader.indicator.adx;


import artskif.trader.candle.Candle1m;
import artskif.trader.candle.CandleType;
import artskif.trader.common.Buffer;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.AbstractIndicator;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Startup
@ApplicationScoped
@NoArgsConstructor(force = true)
public class AdxIndicator1m extends AbstractIndicator<AdxPoint> {

    protected final AdxRepository adxBufferRepository;
    protected final Candle1m candle1m;

    @Inject
    public AdxIndicator1m(AdxRepository adxBufferRepository, Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.adxBufferRepository = adxBufferRepository;
        this.candle1m = candle1m;
    }

    private final Buffer<AdxPoint> buffer = new Buffer<>(Duration.ofMinutes(1), 100);
    private final Path pathForSave = Paths.get("adxIndicator1m.json");

    @Override
    protected CandleType getCandleType() {
        return CandleType.CANDLE_1M;
    }

    @Override
    protected void process(CandleEvent ev) {
        CandlestickDto c = ev.candle();
        Instant bucket = ev.bucket();
        Map<Instant, CandlestickDto> history = candle1m.getBuffer().getSnapshot();
        Optional<AdxPoint> point = AdxCalculator.computeLastAdx(history, true);
        point.ifPresent(p -> buffer.putItem(bucket, p));

        System.out.println("📥 [" + getName() + "] Свеча - " + c);
        System.out.println("📥 [" + getName() + "] Получено новое значение  ADX - " + point.orElse(null));

        // коммитим новое состояние ТОЛЬКО если свеча подтверждена (внутри calc уже учтено)
        if (Boolean.TRUE.equals(c.getConfirmed())) {
            saveBuffer();
        }
    }

    @Override
    public Buffer<AdxPoint> getBuffer() {
        return buffer;
    }

    @Override
    public String getName() {
        return "1m-ADX";
    }

    @Override
    public Path getPathForSave() {
        return pathForSave;
    }

    @Override
    public AdxRepository getBufferRepository() {
        return adxBufferRepository;
    }
}
