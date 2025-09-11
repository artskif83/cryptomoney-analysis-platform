package artskif.trader.indicator.adx;


import artskif.trader.candle.Candle1m;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.common.Buffer;
import artskif.trader.common.BufferRepository;
import artskif.trader.common.PointState;
import artskif.trader.common.StateRepository;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.AbstractIndicator;
import artskif.trader.indicator.IndicatorType;
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

    private final BufferRepository<AdxPoint> adxBufferRepository;
    private final Candle1m candle1m;
    private final Buffer<AdxPoint> buffer;
    private final Path pathForSave = Paths.get("adxIndicator1m.json");

    Instant bucket;
    Instant ts;
    BigDecimal value;
    BigDecimal lastValue;
    Integer period; // Период индикатора


    public AdxIndicator1m(Integer period, ObjectMapper objectMapper, Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.buffer = new Buffer<>(String.format("%s-%dp", NAME, period), Duration.ofMinutes(1), 100);
        this.adxBufferRepository = new BufferRepository<>(objectMapper, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, AdxPoint.class));
        this.candle1m = candle1m;
        this.bucket = null;
        this.period = period;
    }

    @Override
    protected void process(CandleEvent ev) {
        CandlestickDto c = ev.candle();
        Instant bucket = ev.bucket();
        this.bucket = bucket;
        this.ts = Instant.now();

        Map<Instant, CandlestickDto> history = candle1m.getBuffer().getSnapshot();
        Optional<AdxPoint> point = AdxCalculator.computeLastAdx(period, history, true);
        point.ifPresent(p -> {
            value = p.getAdx();
            buffer.putItem(bucket, p);
        });


        // коммитим новое состояние ТОЛЬКО если свеча подтверждена (внутри calc уже учтено)
        if (Boolean.TRUE.equals(c.getConfirmed())) {
            System.out.println("📥 [" + getName() + "] Получено новое значение  ADX - " + point.orElse(null));
            point.ifPresent(p -> lastValue = p.getAdx());
            saveBuffer();
        }
    }

    @Override
    public CandleTimeframe getCandleTimeframe() {
        return CandleTimeframe.CANDLE_1M;
    }

    @Override
    public Integer getPeriod() {
        return period;
    }

    @Override
    public Instant getBucket() {
        return bucket;
    }

    @Override
    public Instant getTs() {
        return ts;
    }

    @Override
    public Buffer<AdxPoint> getBuffer() {
        return buffer;
    }

    @Override
    public String getName() {
        return String.format("%s-%dp", NAME, period);
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
        return value;
    }

    @Override
    public BigDecimal getLastValue() {
        return lastValue;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.ADX;
    }
}
