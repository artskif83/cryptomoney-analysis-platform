package artskif.trader.indicator.rsi;

import artskif.trader.candle.Candle1m;
import artskif.trader.candle.CandleType;
import artskif.trader.common.Buffer;
import artskif.trader.common.BufferRepository;
import artskif.trader.common.PointState;
import artskif.trader.common.StateRepository;
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


@Startup
@ApplicationScoped
@NoArgsConstructor(force = true)
public class RsiIndicator1m  extends AbstractIndicator<RsiPoint> {

    private static final int DEFAULT_PERIOD = 14;

    private final Buffer<RsiPoint> buffer = new Buffer<>(Duration.ofMinutes(1), 100);
    private final Path pathForSave = Paths.get("rsiIndicator1m.json");
    private final Path pathForStateSave = Paths.get("rsiStateIndicator1m.json");

    BufferRepository<RsiPoint> rsiBufferRepository;
    StateRepository rsiStateRepository;
    Candle1m candle1m;
    // состояние RSI + его репозиторий/путь
    private RsiState rsiState = RsiState.empty(DEFAULT_PERIOD);

    @Inject
    public RsiIndicator1m(ObjectMapper objectMapper, Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.rsiBufferRepository = new BufferRepository<>(objectMapper, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, RsiPoint.class));
        this.rsiStateRepository = new StateRepository(objectMapper, objectMapper.getTypeFactory()
                .constructType(RsiState.class));
        this.candle1m = candle1m;
    }

    @Override
    protected CandleType getCandleType() {
        return CandleType.CANDLE_1M;
    }

    @Override
    protected void process(CandleEvent ev) {

        CandlestickDto c = ev.candle();
        Instant bucket = ev.bucket();

        // 1) PREVIEW для текущего тика (если уже инициализированы)
        RsiCalculator.preview(rsiState, c.getClose())
                .ifPresent(rsi -> buffer.putItem(bucket, new RsiPoint(bucket, rsi)));

        // 2) Если свеча подтвердилась — коммитим состояние и кладём финальную точку
        if (Boolean.TRUE.equals(c.getConfirmed())) {
            RsiCalculator.RsiUpdate upd = RsiCalculator.updateConfirmed(rsiState, bucket, c.getClose());
            this.rsiState = upd.state;

            //System.out.println("📥 [" + getName() + "] Получено новое значение  RSI - " + upd.point);

            upd.point.ifPresent(p -> buffer.putItem(bucket, p));

            // сохраняем индикаторный ряд
            saveBuffer();
            // сохраняем состояние RSI
            saveState();
        }
    }

    @Override
    protected StateRepository getStateRepository() {
        return rsiStateRepository;
    }

    @Override
    protected Path getPathForStateSave() {
        return pathForStateSave;
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

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public PointState getState() {
        return rsiState;
    }
}
