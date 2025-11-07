package artskif.trader.indicator.rsi;

import artskif.trader.buffer.Buffer;
import artskif.trader.candle.AbstractCandle;
import artskif.trader.candle.Candle1m;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.AbstractIndicator;
import artskif.trader.repository.BufferRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class RsiAbstractIndicator extends AbstractIndicator<RsiPoint> {

    protected final Buffer<RsiPoint> buffer; // Буфер для хранения точек индикатора

    protected RsiState rsiState; // состояние RSI
    protected Long candleBufferVersion; // Версия буфера свечей, для отслеживания изменений
    protected Integer period; // Период индикатора
    protected BufferRepository<RsiPoint> rsiBufferRepository;
    protected AbstractCandle candle;
    protected BigDecimal currentValue;
    protected BigDecimal confirmedValue;
    protected Instant bucket;
    protected Instant processingTime;

    public RsiAbstractIndicator(CandleEventBus bus, Integer period, BufferRepository<RsiPoint> rsiBufferRepository,
                                int bufferSize, AbstractCandle candle) {
        super(bus);
        this.period = period;
        this.rsiBufferRepository = rsiBufferRepository; // Размер буфера для хранения точек индик
        this.buffer = new Buffer<>(bufferSize);
        this.candle = candle;
        this.candleBufferVersion = 0L;
        this.rsiState =RsiState.empty(period, getCandleTimeframe());
    }

    @Override
    protected void handleEvent(CandleEvent ev) {
        CandlestickDto c = ev.candle();
        Instant bucket = ev.bucket();

        this.bucket = bucket;
        this.processingTime = Instant.now();

        Buffer<CandlestickDto> candleBuffer = candle.getBuffer();
        // Если версия буфера свечей изменилась — пересчитываем индикатор из буфера
        if (candleBufferVersion != candleBuffer.getVersion() && !candleBuffer.isEmpty()) {
            log().infof("📥 [%s] версия буфера свечей изменилась — пересчитываем индикатор из буфера", getName());
            recalculateIndicator(candleBuffer.getSnapshot());
            candleBufferVersion = candleBuffer.getVersion();
        }

        if (isObsoleteCandle(bucket)) return;

        if (Boolean.TRUE.equals(c.getConfirmed())) {
            handleConfirmedCandle(c, bucket);
        } else {
            handleTickCandle(c, bucket);
        }
    }

    protected void handleTickCandle(CandlestickDto c, Instant bucket) {

        RsiCalculator.preview(rsiState, c.getClose())
                .ifPresent(rsi -> {
                            currentValue = rsi;
                        }
                );
    }

    protected void handleConfirmedCandle(CandlestickDto c, Instant bucket) {

        RsiCalculator.RsiUpdate upd = RsiCalculator.updateConfirmed(rsiState, bucket, c.getClose());
        this.rsiState = upd.state;

        log().debugf("📥 [%s] Получено новое значение  RSI - %s", getName(), upd.point);
        log().debugf("📥 [%s] Получено новое значение  State RSI - %s", getName(), upd.state);

        upd.point.ifPresent(p -> {
            currentValue = p.getRsi();
            confirmedValue = p.getRsi();
            buffer.putItem(bucket, p);
        });

        // сохраняем индикаторный ряд
        initSaveBuffer();
    }

    private boolean isObsoleteCandle(Instant bucket) {
        // Пропускаем, если bucket меньше или равен timestamp rsiState
        if (rsiState.getTimestamp() != null && bucket.compareTo(rsiState.getTimestamp()) <= 0) {
            log().debugf("📥 [%s] пропускаем свечи которые раньше текущего состояния. State - %s, bucket - %s",
                    getName(), rsiState.getTimestamp(), bucket);
            return true;
        }
        return false;
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
}
