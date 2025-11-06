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
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@ApplicationScoped
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

    // Конструктор без параметров для CDI
    protected RsiIndicator1m() {
        super(null);
        this.candle1m = null;
        this.buffer = new Buffer<>(100);
        this.period = 14;
        this.candleBufferVersion = 0L;
        this.rsiState = RsiState.empty(period, CandleTimeframe.CANDLE_1M);
    }

    @Inject
    public RsiIndicator1m(Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.rsiBufferRepository = new RsiIndicatorRepository();
        this.candle1m = candle1m;
        this.period = 14;
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

        // Пропускаем, если bucket меньше или равен timestamp rsiState
        if (rsiState.getTimestamp() != null && bucket.compareTo(rsiState.getTimestamp()) <= 0) {
            log().debugf("📥 [%s] пропускаем свечи которые раньше текущего состояния. State - %s, bucket - %s",
                    getName(), rsiState.getTimestamp(), bucket);
            return;
        }

        Buffer<CandlestickDto> candleBuffer = candle1m.getBuffer();
        // 1) Если версия буфера свечей изменилась — пересчитываем индикатор из буфера
        if (candleBufferVersion != candleBuffer.getVersion() && !candleBuffer.isEmpty()) {
            log().infof("📥 [%s] версия буфера свечей изменилась — пересчитываем индикатор из буфера", getName());
            recalculateIndicator(candleBuffer.getSnapshot());
            candleBufferVersion = candleBuffer.getVersion();
        }

        calculateCurrentValue(c);

        // 3) Если свеча подтвердилась — коммитим состояние и кладём финальную точку
        if (Boolean.TRUE.equals(c.getConfirmed())) {
            RsiCalculator.RsiUpdate upd = RsiCalculator.updateConfirmed(rsiState, bucket, c.getClose());
            this.rsiState = upd.state;

            log().debugf("📥 [%s] Получено новое значение  RSI - %s", getName(), upd.point);
            log().debugf("📥 [%s] Получено новое значение  State RSI - %s", getName(), upd.state);

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
            // Обнуляем текущее состояние buffer и rsiState
            buffer.clear();
            rsiState = RsiState.empty(period, CandleTimeframe.CANDLE_1M);

            // Фильтруем только подтверждённые свечи и сортируем по времени
            List<Map.Entry<Instant, CandlestickDto>> confirmedAsc = snap.entrySet().stream()
                    .filter(e -> Boolean.TRUE.equals(e.getValue().getConfirmed()))
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList());

            if (!confirmedAsc.isEmpty()) {
                // Выполняем полный пересчет всех значений RSI
                RsiCalculator.FullRecalculationResult result =
                        RsiCalculator.recalculateFromSnapshot(rsiState, confirmedAsc);

                // Обновляем состояние
                rsiState = result.finalState;

                // Заполняем buffer пересчитанными точками
                for (RsiPoint point : result.points) {
                    buffer.putItem(point.getBucket(), point);
                }

                log().infof("📥 [%s] RSI индикатор полностью пересчитан из истории свечей. " +
                                "Восстановлено точек: %d, финальное состояние: %s",
                        getName(), result.points.size(), rsiState);
            } else {
                log().warnf("📥 [%s] Буфер свечей не содержит подтвержденных данных", getName());
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