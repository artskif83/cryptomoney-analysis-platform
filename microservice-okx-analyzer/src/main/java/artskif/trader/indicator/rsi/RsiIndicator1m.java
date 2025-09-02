package artskif.trader.indicator.rsi;

import artskif.trader.candle.Candle1m;
import artskif.trader.candle.CandleType;
import artskif.trader.common.Buffer;
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

@Startup
@ApplicationScoped
@NoArgsConstructor(force = true)
public class RsiIndicator1m  extends AbstractIndicator<RsiPoint> {

    RsiRepository rsiBufferRepository;
    Candle1m candle1m;

    @Inject
    public RsiIndicator1m(RsiRepository rsiBufferRepository, Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.rsiBufferRepository = rsiBufferRepository;
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
        System.out.println("📥 [" + getName() + "] расчет RSI - ");
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
    public RsiRepository getBufferRepository() {
        return rsiBufferRepository;
    }
}
