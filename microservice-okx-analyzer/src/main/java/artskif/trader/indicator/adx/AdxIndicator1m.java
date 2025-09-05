package artskif.trader.indicator.adx;


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

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class AdxIndicator1m extends AbstractIndicator<AdxPoint> {

    private final static String NAME = "ADX-1m";
    private static final int DEFAULT_PERIOD = 14;

    protected final BufferRepository<AdxPoint> adxBufferRepository;
    protected final Candle1m candle1m;
    private final Buffer<AdxPoint> buffer;
    private final Path pathForSave = Paths.get("adxIndicator1m.json");

    public AdxIndicator1m(ObjectMapper objectMapper, Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.buffer = new Buffer<>(String.format("%s-%dp", NAME, DEFAULT_PERIOD), Duration.ofMinutes(1), 100);
        this.adxBufferRepository = new BufferRepository<>(objectMapper, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, AdxPoint.class));
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
        Map<Instant, CandlestickDto> history = candle1m.getBuffer().getSnapshot();
        Optional<AdxPoint> point = AdxCalculator.computeLastAdx(DEFAULT_PERIOD, history, true);
        point.ifPresent(p -> buffer.putItem(bucket, p));

        //System.out.println("📥 [" + getName() + "] Получено новое значение  ADX - " + point.orElse(null));

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
    public BufferRepository<AdxPoint>  getBufferRepository() {
        return adxBufferRepository;
    }

    @Override
    public boolean isStateful() {
        return false;
    }

    @Override
    public PointState getState() {
        return null;
    }

    @Override
    protected StateRepository getStateRepository() {
        return null;
    }

    @Override
    protected Path getPathForStateSave() {
        return null;
    }

    @Override
    public BigDecimal getValue() {
        return null;
    }
}
