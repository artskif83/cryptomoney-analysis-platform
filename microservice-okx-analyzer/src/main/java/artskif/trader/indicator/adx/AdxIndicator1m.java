package artskif.trader.indicator.adx;


import artskif.trader.buffer.Buffer;
import artskif.trader.candle.Candle1m;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.common.PointState;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.AbstractIndicator;
import artskif.trader.indicator.IndicatorType;
import artskif.trader.repository.AdxIndicatorRepository;
import artskif.trader.repository.BufferRepository;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdxIndicator1m extends AbstractIndicator<AdxPoint> {

    private final static String NAME = "ADX-1m";
    private final static Logger LOG = Logger.getLogger(AdxIndicator1m.class);

    private final Duration interval = Duration.ofMinutes(1);
    private final Duration acceptableTimeMargin = Duration.ofSeconds(5);
    private final Buffer<AdxPoint> buffer;
    private final BufferRepository<AdxPoint> adxBufferRepository;
    private final Candle1m candle1m;

    private Integer period;
    private AdxState adxState;
    private BigDecimal value;
    private BigDecimal lastValue;
    private Instant bucket;
    private Instant processingTime;

    public AdxIndicator1m(Integer period, Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.period = period;
        this.candle1m = candle1m;
        this.buffer = new Buffer<>(100);
        this.adxBufferRepository = new AdxIndicatorRepository();
        this.adxState = AdxState.empty(period);
        this.period = period;
    }

    @Override
    protected void process(CandleEvent ev) {

        CandlestickDto c = ev.candle();
        Instant bucket = ev.bucket();
        this.bucket = bucket;
        this.processingTime = Instant.now();

        Instant currentBucket = Instant.now().minus(interval).minus(acceptableTimeMargin);
        if (bucket.isBefore(currentBucket)) return; // берём только свежие
        if (this.adxState != null && adxState.getTimestamp() != null && !bucket.minus(interval).equals(adxState.getTimestamp())) {
            this.adxState = AdxState.empty(period);
            log().infof("📥 [%s] Сбрасываем состояние ADX из-за потери актуальности - %s", getName(), adxState);
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
                        log().infof("📥 [%s] Состояние ADX восстановлено из буфера - %s", getName(), adxState);
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

            log().infof("📥 [%s] Получено новое значение ADX - %s", getName(), upd.point);
            log().infof("📥 [%s] Новое состояние ADX - %s", getName(), upd.state);

            upd.point.ifPresent(p -> {
                value = p.getAdx();
                lastValue = p.getAdx();
                buffer.putItem(bucket, p);
            });

            // сохраняем ряд и состояние
            initSaveBuffer();
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
    public Instant getProcessingTime() {
        return processingTime;
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
    protected BufferRepository<AdxPoint> getBufferRepository() {
        return adxBufferRepository;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public PointState getState() {
        return adxState;
    }

    @Override
    public BigDecimal getValue() {
        return value;
    }

    @Override
    public BigDecimal getConfirmedValue() {
        return lastValue;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.ADX;
    }

    @Override
    public Logger log() {
        return LOG;
    }
}
