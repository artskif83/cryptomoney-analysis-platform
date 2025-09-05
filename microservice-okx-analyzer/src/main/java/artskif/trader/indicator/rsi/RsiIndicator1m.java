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

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;


public class RsiIndicator1m extends AbstractIndicator<RsiPoint> {

    private final Buffer<RsiPoint> buffer = new Buffer<>(Duration.ofMinutes(1), 100);
    private final Duration interval = Duration.ofMinutes(1);
    private final Duration acceptableTimeMargin = Duration.ofSeconds(5); // Допустимая погрешность по времени

    BufferRepository<RsiPoint> rsiBufferRepository;
    StateRepository rsiStateRepository;
    Candle1m candle1m;
    Integer period; // Период индикатора
    // состояние RSI + его репозиторий/путь
    RsiState rsiState;
    BigDecimal value;
    Path pathForSave;
    Path pathForStateSave;


    public RsiIndicator1m(Integer period, ObjectMapper objectMapper, Candle1m candle1m, CandleEventBus bus) {
        super(bus);
        this.rsiBufferRepository = new BufferRepository<>(objectMapper, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, RsiPoint.class));
        this.rsiStateRepository = new StateRepository(objectMapper, objectMapper.getTypeFactory()
                .constructType(RsiState.class));
        this.candle1m = candle1m;
        this.period = period;
        this.rsiState = RsiState.empty(period);
        this.pathForSave = Paths.get(MessageFormat.format("rsiIndicator1m{0}p.json", period));
        this.pathForStateSave = Paths.get(MessageFormat.format("rsiStateIndicator1m{0}p.json", period));
    }

    @Override
    protected CandleType getCandleType() {
        return CandleType.CANDLE_1M;
    }

    @Override
    protected void process(CandleEvent ev) {

        CandlestickDto c = ev.candle();
        Instant bucket = ev.bucket();
        Instant currentBucket = Instant.now().minus(interval).minus(acceptableTimeMargin);
        if (bucket.isBefore(currentBucket)) return;// Нас интересуют только "свежие" свечи
        if (this.rsiState != null && rsiState.getTimestamp() != null && !bucket.minus(interval).equals(rsiState.getTimestamp())) {
            this.rsiState = RsiState.empty(period);
            System.out.println("📥 [" + getName() + "] Сбрасываем состояние RSI из-за потери актуальности - " + rsiState);
        }

        // 1) PREVIEW для текущего тика (если уже инициализированы)
        RsiCalculator.preview(rsiState, c.getClose())
                .ifPresent(rsi -> {
                            value = rsi;
                            buffer.putItem(bucket, new RsiPoint(bucket, rsi));
                        }
                );

        // 2) Если свеча подтвердилась — коммитим состояние и кладём финальную точку
        if (Boolean.TRUE.equals(c.getConfirmed())) {
            RsiCalculator.RsiUpdate upd = RsiCalculator.updateConfirmed(rsiState, bucket, c.getClose());
            this.rsiState = upd.state;

            System.out.println("📥 [" + getName() + "] Получено новое значение  RSI - " + upd.point);
            System.out.println("📥 [" + getName() + "] Получено новое значение  State - " + upd.state);

            upd.point.ifPresent(p -> {
                value = p.getRsi();
                buffer.putItem(bucket, p);
            });

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

    @Override
    public BigDecimal getValue() {
        return value;
    }
}
