package artskif.trader.candle;

import artskif.trader.events.CandleEventBus;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@ApplicationScoped
public class Candle1D extends AbstractTimeSeriesTicker {

    @Inject
    CandleBufferRepository candleBufferRepository;
    @Inject
    CandleEventBus bus;

    private final CandleBuffer buffer = new CandleBuffer(Duration.ofDays(1), 300);
    private final Path pathForSave = Paths.get("candles1d.json");

    @PostConstruct
    void init() {
        restoreBuffer();
    }


    @Override
    protected CandleType getCandleType() {
        return CandleType.CANDLE_1D;
    }

    @Override
    public CandleBuffer getBuffer() {
        return buffer;
    }

    @Override
    public String getName() {
        return "1D-candle";
    }

    @Override
    public Path getPathForSave() {
        return pathForSave;
    }

    @Override
    public CandleBufferRepository getBufferRepository() {
        return candleBufferRepository;
    }

    @Override
    protected CandleEventBus getEventBus() {
        return bus;
    }

}
