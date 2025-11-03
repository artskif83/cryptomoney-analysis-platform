package artskif.trader.indicator.rsi;

import artskif.trader.candle.Candle1m;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.buffer.Buffer;
import artskif.trader.common.PointState;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.AbstractIndicator;
import artskif.trader.indicator.IndicatorType;
import artskif.trader.repository.BufferRepository;
import artskif.trader.repository.RsiIndicatorRepository;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class RsiIndicator1m extends AbstractIndicator<RsiPoint> {

    private final static String NAME = "RSI-1m";
    private final static Logger LOG = Logger.getLogger(RsiIndicator1m.class);

    private final Buffer<RsiPoint> buffer; // Допустимая погрешность по времени

    private Long candleBufferVersion;
    private BufferRepository<RsiPoint> rsiBufferRepository;
    private Candle1m candle1m;
    private Integer period; // Период индикатора
    private RsiState rsiState; // состояние RSI
    private BigDecimal value;
    private BigDecimal confirmedValue;
    private Instant bucket;
    private Instant processingTime;

    public RsiIndicator1m(Integer period, Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.rsiBufferRepository = new RsiIndicatorRepository();
        this.candle1m = candle1m;
        this.period = period;
        this.bucket = null;
        this.rsiState = RsiState.empty(period, CandleTimeframe.CANDLE_1M);
        this.buffer = new Buffer<>(100);
        this.candleBufferVersion = 0L;
    }

    @Override
    protected void process(CandleEvent ev) {
        CandlestickDto c = ev.candle();
        Instant bucket = ev.bucket();
        this.bucket = bucket;
        this.processingTime = Instant.now();
        Buffer<CandlestickDto> candleBuffer = candle1m.getBuffer();
        // 1) Если состояние ещё не готово — пытаемся поднять его из истории минутных свечей
        if (candleBufferVersion != candleBuffer.getVersion() && !candleBuffer.isEmpty()) {
            recalculateIndicator(candleBuffer.getSnapshot());
            candleBufferVersion = candleBuffer.getVersion();
        }

        calculateCurrentValue(c);

        // 3) Если свеча подтвердилась — коммитим состояние и кладём финальную точку
        if (Boolean.TRUE.equals(c.getConfirmed())) {
            RsiCalculator.RsiUpdate upd = RsiCalculator.updateConfirmed(rsiState, bucket, c.getClose());
            this.rsiState = upd.state;

            log().infof("📥 [%s] Получено новое значение  RSI - %s", getName(), upd.point);
            log().infof("📥 [%s] Получено новое значение  State RSI - %s", getName(), upd.state);

            upd.point.ifPresent(p -> {
                value = p.getRsi();
                confirmedValue = p.getRsi();
                buffer.putItem(bucket, p);
            });

            // сохраняем индикаторный ряд
            initSaveBuffer();
        }
    }

    private void calculateCurrentValue(CandlestickDto c) {
        // 2) PREVIEW для текущего тика (если уже инициализированы)
        RsiCalculator.preview(rsiState, c.getClose())
                .ifPresent(rsi -> {
                            value = rsi;
                        }
                );
    }

    private void recalculateIndicator(Map<Instant, CandlestickDto> snap) {

        if (snap != null && !snap.isEmpty()) {
            // подтверждённые ↑
            List<Map.Entry<Instant, CandlestickDto>> confirmedAsc = snap.entrySet().stream()
                    .filter(e -> Boolean.TRUE.equals(e.getValue().getConfirmed()))
                    .collect(Collectors.toList());

            if (!confirmedAsc.isEmpty()) {
                // берём только хвост длиной <= period
                int size = confirmedAsc.size();
                int from = Math.max(0, size - period - 1);
                List<Map.Entry<Instant, CandlestickDto>> tailAsc = confirmedAsc.subList(from, size);

                rsiState = RsiCalculator.tryInitFromHistory(rsiState, tailAsc);
                if (rsiState != null)
                    log().infof("📥 [%s] Значение RSI восстановлено из истории свечей - %s", getName(), rsiState);
            } else {
                log().warnf("📥 [%s] Буфер свечей пуст", getName());
            }
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
    public Buffer<RsiPoint> getBuffer() {
        return buffer;
    }

    @Override
    public String getName() {
        return String.format("%s-%dp", NAME, period);
    }

    @Override
    protected BufferRepository<RsiPoint> getBufferRepository() {
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

    @Override
    public BigDecimal getValue() {
        return value;
    }

    @Override
    public BigDecimal getConfirmedValue() {
        return confirmedValue;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.RSI;
    }

    @Override
    public Logger log() {
        return LOG;
    }
}
