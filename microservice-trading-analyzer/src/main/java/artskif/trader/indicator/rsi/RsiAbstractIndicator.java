package artskif.trader.indicator.rsi;

import artskif.trader.buffer.TimeSeriesBuffer;
import artskif.trader.candle.AbstractCandle;
import artskif.trader.common.Stage;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.dto.RsiPointDto;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.AbstractIndicator;
import artskif.trader.repository.BufferRepository;
import jakarta.enterprise.inject.Instance;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class RsiAbstractIndicator extends AbstractIndicator<RsiPointDto> {

    protected final TimeSeriesBuffer<RsiPointDto> rsiLiveBuffer; // Буфер для хранения актуальных точек RSI
    protected final TimeSeriesBuffer<RsiPointDto> rsiHistoricalBuffer; // Буфер для хранения исторических точек RSI

    protected AtomicInteger rsiLiveVersion; // версия буфера текущего RSI
    protected AtomicInteger rsiHistoricalVersion; // версия буфера исторического RSI
    protected RsiState rsiLiveState; // состояние RSI
    protected Integer period; // Период индикатора
    protected BufferRepository<RsiPointDto> rsiBufferRepository;
    protected AbstractCandle candle;
    protected Instant bucket;
    protected Instant lastProcessingTime;
    protected List<Stage<RsiPipelineContext>> metrics;

    public RsiAbstractIndicator(AbstractCandle candle, CandleEventBus bus, Instance<Stage<RsiPipelineContext>> metrics, Integer period, BufferRepository<RsiPointDto> rsiBufferRepository,
                                int bufferSize, int bufferHistoricalSize) {
        super(bus);
        this.period = period;
        this.rsiBufferRepository = rsiBufferRepository; // Размер буфера для хранения точек индик
        this.rsiLiveBuffer = new TimeSeriesBuffer<>(bufferSize);
        this.rsiHistoricalBuffer = new TimeSeriesBuffer<>(bufferHistoricalSize);
        this.candle = candle;
        this.rsiLiveState = RsiState.empty(period, getCandleTimeframe());
        this.metrics = metrics != null ? metrics.stream()
                .sorted(Comparator.comparingInt(Stage::order))
                .toList() : List.of();
        rsiLiveVersion = new AtomicInteger(0);
        rsiHistoricalVersion = new AtomicInteger(0);
    }

    @Override
    protected void handleHistoryEvent(CandleEvent take) {

        TimeSeriesBuffer<CandlestickDto> candleHistoricalBuffer = candle.getHistoricalBuffer();

        if (rsiHistoricalVersion.get() != candleHistoricalBuffer.getVersion().get()) {
            recalculateForBuffer("Исторический буфер", rsiHistoricalBuffer, candleHistoricalBuffer);
            initSaveHistoricalBuffer();

            rsiHistoricalVersion.set(candleHistoricalBuffer.getVersion().get());
            log().infof("📥 [%s] Исторический буфер пересчитан. RSI буфер: [%s - %s], Candle буфер: [%s - %s]",
                    getName(),
                    rsiHistoricalBuffer.getFirstBucket(), rsiHistoricalBuffer.getLastBucket(),
                    candleHistoricalBuffer.getFirstBucket(), candleHistoricalBuffer.getLastBucket());

        }
    }

    @Override
    protected void handleTickEvent(CandleEvent ev) {
        this.lastProcessingTime = Instant.now();

        TimeSeriesBuffer<CandlestickDto> candleLiveBuffer = candle.getLiveBuffer();

        RsiPipelineContext context;

        if (rsiLiveVersion.get() != candleLiveBuffer.getVersion().get()) {
            recalculateForBuffer("Актуальный буфер", rsiLiveBuffer, candleLiveBuffer);
            rsiLiveVersion.set(candleLiveBuffer.getVersion().get());
            log().infof("📥 [%s] Актуальный буфер пересчитан. RSI буфер: [%s - %s], Candle буфер: [%s - %s]",
                    getName(),
                    rsiLiveBuffer.getFirstBucket(), rsiLiveBuffer.getLastBucket(),
                    candleLiveBuffer.getFirstBucket(), candleLiveBuffer.getLastBucket());
        } else {
            context = recalculateForCandle(rsiLiveState, ev.bucket(), ev.candle());
            if (context.point() != null) {
                rsiLiveBuffer.putItem(context.point().getBucket(), context.point());
                rsiHistoricalBuffer.putItem(context.point().getBucket(), context.point());
            }
            rsiLiveState = context.state();
        }

        initSaveLiveBuffer();
    }

    private RsiPipelineContext recalculateForBuffer(
            String bufferDescription,
            TimeSeriesBuffer<RsiPointDto> rsiBuffer,
            TimeSeriesBuffer<CandlestickDto> candleBuffer) {

        if (candleBuffer.isEmpty() || candleBuffer.getLastBucket() == null) {
            log().infof("📥 [%s] %s свечей пустой, пропускаем пересчет исторического RSI индикатора", getName(), bufferDescription);
            return null;
        }

        log().infof("📥 [%s] %s пересчитывается. RSI буфер: [%s - %s], Candle буфер: [%s - %s]",
                getName(), bufferDescription,
                rsiBuffer.getFirstBucket(), rsiBuffer.getLastBucket(),
                candleBuffer.getFirstBucket(), candleBuffer.getLastBucket());

        RsiState rsiState = RsiState.empty(period, getCandleTimeframe());

        Map<Instant, CandlestickDto> candleItems = candleBuffer.getItemsBetween(null, null);
        RsiPipelineContext context = null;

        for (Map.Entry<Instant, CandlestickDto> entry : candleItems.entrySet()) {
            context = recalculateForCandle(rsiState, entry.getKey(), entry.getValue());
            rsiState = context.state();

            if (context.point() != null && !rsiBuffer.containsKey(context.point().getBucket())) {
                rsiBuffer.putItem(context.point().getBucket(), context.point());
            }
        }

        return context;
    }

    private RsiPipelineContext recalculateForCandle(RsiState rsiState, Instant candleBucket, CandlestickDto candleDto) {
        // Проверяем, что candleBucket является следующим ожидаемым бакетом после текущего состояния
        if (rsiState.getTimestamp() != null) {
            Instant expectedNextBucket = rsiState.getTimestamp().plus(rsiState.getTimeframe().getDuration());
            if (!candleBucket.equals(expectedNextBucket)) {
                log().warnf("⚠️ [%s] Обнаружен разрыв в последовательности бакетов. Ожидался: %s, получен: %s. Сброс состояния RSI.",
                        getName(), expectedNextBucket, candleBucket);
                rsiState = RsiState.empty(period, getCandleTimeframe());
            }
        }

        RsiPipelineContext context = new RsiPipelineContext(rsiState, null, candleBucket, candleDto);

        for (Stage<RsiPipelineContext> metric : this.metrics) {
            context = metric.process(context);
        }

        return context;
    }
}

