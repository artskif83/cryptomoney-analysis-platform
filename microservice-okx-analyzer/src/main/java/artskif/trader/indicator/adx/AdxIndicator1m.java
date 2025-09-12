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
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdxIndicator1m extends AbstractIndicator<AdxPoint> {

    private final static String NAME = "ADX-1m";

    private final Duration interval = Duration.ofMinutes(1);
    private final Duration acceptableTimeMargin = Duration.ofSeconds(5);
    private final Buffer<AdxPoint> buffer;

    private final BufferRepository<AdxPoint> adxBufferRepository;
    private final StateRepository adxStateRepository;
    private final Candle1m candle1m;
    private final Path pathForSave;
    private final Path pathForStateSave;

    private Integer period;
    private AdxState adxState;
    private BigDecimal value;
    private BigDecimal lastValue;
    private Instant bucket;
    private Instant ts;


    public AdxIndicator1m(Integer period, ObjectMapper objectMapper, Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.period = period;
        this.candle1m = candle1m;

        this.buffer = new Buffer<>(String.format("%s-%dp", NAME, period), Duration.ofMinutes(1), 100);
        this.adxBufferRepository = new BufferRepository<>(objectMapper, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, AdxPoint.class));
        this.adxStateRepository = new StateRepository(objectMapper, objectMapper.getTypeFactory()
                .constructType(AdxState.class));

        this.adxState = AdxState.empty(period);
        this.pathForSave = Paths.get(MessageFormat.format("adxIndicator1m{0}p.json", period));
        this.pathForStateSave = Paths.get(MessageFormat.format("adxStateIndicator1m{0}p.json", period));

        this.period = period;
    }

    @Override
    protected void process(CandleEvent ev) {

        CandlestickDto c = ev.candle();
        Instant bucket = ev.bucket();
        this.bucket = bucket;
        this.ts = Instant.now();

        Instant currentBucket = Instant.now().minus(interval).minus(acceptableTimeMargin);
        if (bucket.isBefore(currentBucket)) return; // берём только свежие
        if (this.adxState != null && adxState.getTimestamp() != null && !bucket.minus(interval).equals(adxState.getTimestamp())) {
            this.adxState = AdxState.empty(period);
            System.out.println("📥 [" + getName() + "] Сбрасываем состояние ADX из-за потери актуальности - " + adxState);
        }

        // 1) init из подтверждённой истории (как в RSI-версии)
        if (adxState != null && adxState.getTimestamp() == null && !adxState.isInitialized()) {
            Map<Instant, CandlestickDto> snap = candle1m.getBuffer().getSnapshot();
            if (snap != null && !snap.isEmpty()) {
                List<Map.Entry<Instant, CandlestickDto>> confirmedAsc = snap.entrySet().stream()
                        .filter(e -> Boolean.TRUE.equals(e.getValue().getConfirmed()))
                        .collect(Collectors.toList());

                if (!confirmedAsc.isEmpty()) {
                    int size = confirmedAsc.size();
                    int from = Math.max(0, size - (period * 2) - 2);
                    List<Map.Entry<Instant, CandlestickDto>> tailAsc = confirmedAsc.subList(from, size);

                    adxState = AdxCalculator.tryInitFromHistory(adxState, tailAsc);
                    if (adxState != null)
                        System.out.println("📥 [" + getName() + "] Состояние ADX восстановлено из истории - " + adxState);
                }
            }
        }

        // 2) PREVIEW на текущем тике
        AdxCalculator.preview(adxState, c).ifPresent(adx -> {
            value = adx;
            buffer.putItem(bucket, new AdxPoint(bucket, adx));
        });

        // 3) commit при подтверждении свечи
        if (Boolean.TRUE.equals(c.getConfirmed())) {
            AdxCalculator.AdxUpdate upd = AdxCalculator.updateConfirmed(adxState, bucket, c);
            this.adxState = upd.state;

            System.out.println("📥 [" + getName() + "] Получено новое значение ADX - " + upd.point);
            System.out.println("📥 [" + getName() + "] Новое состояние ADX - " + upd.state);

            upd.point.ifPresent(p -> {
                value = p.getAdx();
                lastValue = p.getAdx();
                buffer.putItem(bucket, p);
            });

            // сохраняем ряд и состояние
            saveBuffer();
            saveState();
        }
    }

    @Override public CandleTimeframe getCandleTimeframe() { return CandleTimeframe.CANDLE_1M; }
    @Override public Integer getPeriod() { return period; }
    @Override public Instant getBucket() { return bucket; }
    @Override public Instant getTs() { return ts; }
    @Override public Buffer<AdxPoint> getBuffer() { return buffer; }
    @Override public String getName() { return String.format("%s-%dp", NAME, period); }
    @Override public Path getPathForSave() { return pathForSave; }
    @Override public BufferRepository<AdxPoint> getBufferRepository() { return adxBufferRepository; }

    @Override public boolean isStateful() { return true; }
    @Override public PointState getState() { return adxState; }
    @Override protected StateRepository getStateRepository() { return adxStateRepository; }
    @Override protected Path getPathForStateSave() { return pathForStateSave; }

    @Override public BigDecimal getValue() { return value; }
    @Override public BigDecimal getLastValue() { return lastValue; }
    @Override public IndicatorType getType() { return IndicatorType.ADX; }
}
