package artskif.trader.indicator.rsi;

import artskif.trader.candle.Candle1m;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.buffer.Buffer;
import artskif.trader.buffer.BufferRepository;
import artskif.trader.common.PointState;
import artskif.trader.common.StateRepository;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.AbstractIndicator;
import artskif.trader.indicator.IndicatorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

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


public class RsiIndicator1m extends AbstractIndicator<RsiPoint> {

    private final static String NAME = "RSI-1m";
    private final static Logger LOG = Logger.getLogger(RsiIndicator1m.class);

    private final Buffer<RsiPoint> buffer;
    private final Duration interval = Duration.ofMinutes(1);
    private final Duration acceptableTimeMargin = Duration.ofSeconds(5); // Допустимая погрешность по времени

    BufferRepository<RsiPoint> rsiBufferRepository;
    StateRepository rsiStateRepository;
    Candle1m candle1m;
    Integer period; // Период индикатора
    RsiState rsiState; // состояние RSI + его репозиторий/путь
    BigDecimal value;
    BigDecimal lastValue;
    Path pathForSave;
    Path pathForStateSave;
    Instant bucket;
    Instant ts;

    public RsiIndicator1m(Integer period, ObjectMapper objectMapper, Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.rsiBufferRepository = new BufferRepository<>(objectMapper, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, RsiPoint.class));
        this.rsiStateRepository = new StateRepository(objectMapper, objectMapper.getTypeFactory()
                .constructType(RsiState.class));
        this.candle1m = candle1m;
        this.period = period;
        this.bucket = null;
        this.rsiState = RsiState.empty(period);
        this.pathForSave = Paths.get(MessageFormat.format("rsiIndicator1m{0}p.json", period));
        this.pathForStateSave = Paths.get(MessageFormat.format("rsiStateIndicator1m{0}p.json", period));
        this.buffer = new Buffer<>(String.format("%s-%dp", NAME, period), Duration.ofMinutes(1), 100);
    }

    @Override
    protected void process(CandleEvent ev) {
        CandlestickDto c = ev.candle();
        Instant bucket = ev.bucket();
        this.bucket = bucket;
        this.ts = Instant.now();

        Instant currentBucket = Instant.now().minus(interval).minus(acceptableTimeMargin);
        if (bucket.isBefore(currentBucket)) return;// Нас интересуют только "свежие" свечи
        if (this.rsiState != null && rsiState.getTimestamp() != null && !bucket.minus(interval).equals(rsiState.getTimestamp())) {
            log().infof("📥 [%s] Сбрасываем состояние RSI из-за разницы во времени. Время текущего тика - %s. Время состояния - %s", getName(), bucket.toString(), rsiState.getTimestamp().toString());
            this.rsiState = RsiState.empty(period);
        }

        // 1) Если состояние ещё не готово — пытаемся поднять его из истории минутных свечей
        if (rsiState != null && rsiState.getTimestamp() == null && !rsiState.isInitialized()) {
            Map<Instant, CandlestickDto> snap = candle1m.getBuffer().getSnapshot();

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

        // 2) PREVIEW для текущего тика (если уже инициализированы)
        RsiCalculator.preview(rsiState, c.getClose())
                .ifPresent(rsi -> {
                            value = rsi;
                            buffer.putItem(bucket, new RsiPoint(bucket, rsi));
                        }
                );

        // 3) Если свеча подтвердилась — коммитим состояние и кладём финальную точку
        if (Boolean.TRUE.equals(c.getConfirmed())) {
            RsiCalculator.RsiUpdate upd = RsiCalculator.updateConfirmed(rsiState, bucket, c.getClose());
            this.rsiState = upd.state;

            log().infof("📥 [%s] Получено новое значение  RSI - %s", getName(), upd.point);
            log().infof("📥 [%s] Получено новое значение  State RSI - %s", getName(), upd.state);

            upd.point.ifPresent(p -> {
                value = p.getRsi();
                lastValue = p.getRsi();
                buffer.putItem(bucket, p);
            });

            // сохраняем индикаторный ряд
            saveBuffer();
            // сохраняем состояние RSI
            saveState();
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
        return String.format("%s-%dp", NAME, period);
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
        return IndicatorType.RSI;
    }

    @Override
    public Logger log() {
        return LOG;
    }
}
